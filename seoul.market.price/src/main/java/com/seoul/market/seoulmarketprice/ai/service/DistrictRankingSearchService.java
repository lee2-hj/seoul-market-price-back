package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.DistrictRankingResponse;
import com.seoul.market.seoulmarketprice.ai.dto.RankingCriteria;
import com.seoul.market.seoulmarketprice.fastapi.dto.request.ListRequest;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.ListResponse;
import com.seoul.market.seoulmarketprice.fastapi.service.FastApiService;
import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DistrictRankingSearchService {
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;
    private static final int MINIMUM_TRADE_COUNT = 3;
    private static final Pattern LIMIT = Pattern.compile("(\\d+)\\s*(?:개|곳)");

    private final SggMasterRepository sggRepository;
    private final FastApiService fastApiService;

    public DistrictRankingSearchService(SggMasterRepository sggRepository, FastApiService fastApiService) {
        this.sggRepository = sggRepository;
        this.fastApiService = fastApiService;
    }

    public DistrictRankingResponse search(String question) {
        int limit = limit(question);
        boolean ascending = question.contains("낮") || question.contains("최저")
                || question.contains("저렴") || question.contains("싼");

        List<DistrictSummary> summaries = sggRepository.findAllByOrderBySggNameAsc().parallelStream()
                .map(this::summarize)
                .filter(summary -> summary != null && summary.averagePyeongAmount() != null
                        && summary.dealCount() >= MINIMUM_TRADE_COUNT)
                .toList();
        if (summaries.isEmpty()) {
            throw new IllegalArgumentException("서울시 자치구 평단가 데이터가 없습니다.");
        }

        Comparator<DistrictSummary> comparator = Comparator.comparing(DistrictSummary::averagePyeongAmount);
        if (!ascending) comparator = comparator.reversed();
        List<DistrictSummary> ranked = summaries.stream().sorted(comparator).limit(limit).toList();
        List<DistrictRankingResponse.Item> items = java.util.stream.IntStream.range(0, ranked.size())
                .mapToObj(index -> {
                    DistrictSummary value = ranked.get(index);
                    return new DistrictRankingResponse.Item(index + 1, value.districtName(),
                            value.averagePyeongAmount(), value.dealCount());
                }).toList();
        String baseDate = ranked.stream().map(DistrictSummary::baseDate)
                .filter(value -> value != null && !value.isBlank()).findFirst().orElse(null);
        RankingCriteria criteria = new RankingCriteria("평균 평단가", "만원/평",
                baseDate == null ? "최근 집계 기간" : baseDate + " 기준",
                MINIMUM_TRADE_COUNT, ascending ? "낮은 순" : "높은 순");
        return new DistrictRankingResponse("서울 전체", "district_pyeong", baseDate, criteria, items);
    }

    private DistrictSummary summarize(SggMaster sgg) {
        ListResponse response = fastApiService.getPyeongList(new ListRequest(sgg.getSggCode()));
        if (response == null || response.groups() == null) {
            throw new IllegalArgumentException(sgg.getSggName() + "의 평단가 데이터를 조회할 수 없습니다.");
        }
        long weightedSum = 0;
        long countSum = 0;
        for (ListResponse.ListSummaryDto item : response.groups().values()) {
            long count = item.total_count() == null ? 0 : item.total_count();
            if (item.avg_pyeong_amt() != null && count > 0) {
                weightedSum += item.avg_pyeong_amt() * count;
                countSum += count;
            }
        }
        if (countSum == 0) return null;
        return new DistrictSummary(sgg.getSggName(), Math.round((double) weightedSum / countSum),
                countSum, response.baseDate());
    }

    private int limit(String question) {
        Matcher matcher = LIMIT.matcher(question);
        int requested = matcher.find() ? Integer.parseInt(matcher.group(1)) : DEFAULT_LIMIT;
        if (requested < 1 || requested > MAX_LIMIT) {
            throw new IllegalArgumentException("자치구 순위는 1개부터 10개까지 조회할 수 있습니다.");
        }
        return requested;
    }

    private record DistrictSummary(String districtName, Long averagePyeongAmount, long dealCount, String baseDate) {}
}
