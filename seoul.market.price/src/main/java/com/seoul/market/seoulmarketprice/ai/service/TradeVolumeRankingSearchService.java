package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.RankingMetric;
import com.seoul.market.seoulmarketprice.ai.dto.RankingCriteria;
import com.seoul.market.seoulmarketprice.ai.dto.RankingSearchQuery;
import com.seoul.market.seoulmarketprice.ai.dto.TradeVolumeRankingResponse;
import com.seoul.market.seoulmarketprice.fastapi.dto.request.RttRequest;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.RttRespopnse;
import com.seoul.market.seoulmarketprice.fastapi.service.FastApiService;
import com.seoul.market.seoulmarketprice.location.dto.DongRegionResponse;
import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Comparator;
import java.util.regex.Matcher;

/** rtt 요약 API의 top5_by_volume을 자연어 거래량 순위 검색으로 제공한다. */
@Service
public class TradeVolumeRankingSearchService {
    private static final int FAST_API_MAX_LIMIT = 5;

    private final RankingQuestionParser questionParser;
    private final SggMasterRepository sggRepository;
    private final LocationMasterService locationService;
    private final FastApiService fastApiService;
    private final RankingRegionResolver regionResolver;

    public TradeVolumeRankingSearchService(RankingQuestionParser questionParser,
                                           SggMasterRepository sggRepository,
                                           LocationMasterService locationService,
                                           FastApiService fastApiService) {
        this.questionParser = questionParser;
        this.sggRepository = sggRepository;
        this.locationService = locationService;
        this.fastApiService = fastApiService;
        this.regionResolver = new RankingRegionResolver(sggRepository, locationService);
    }

    public TradeVolumeRankingResponse search(String question) {
        return search(question, questionParser.parse(question));
    }

    public TradeVolumeRankingResponse search(String question, RankingSearchQuery query) {
        if (query.metric() != RankingMetric.TRADE_COUNT) {
            throw new IllegalArgumentException("거래량 순위 질문만 처리할 수 있습니다.");
        }
        if (query.limit() > FAST_API_MAX_LIMIT) {
            throw new IllegalArgumentException("현재 거래량 순위는 상위 5개까지만 제공됩니다.");
        }

        Region region = resolveRegion(question);
        if (region == null) return searchAllSeoul(query);
        RttRespopnse response = fastApiService.getRttInfo(new RttRequest(region.sggCode(), region.dongCode()));
        return toResponse(region.name(), response, query);
    }

    private TradeVolumeRankingResponse searchAllSeoul(RankingSearchQuery query) {
        List<RegionResponse> responses = locationService.getSggs().stream()
                .map(sgg -> new Region(sgg.sggCd(), null, sgg.sggNm()))
                .map(region -> new RegionResponse(region,
                        fastApiService.getRttInfo(new RttRequest(region.sggCode(), null))))
                .toList();
        List<Candidate> candidates = responses.stream()
                .flatMap(item -> candidates(item.region(), item.response()).stream())
                .filter(item -> item.dealCount() >= query.minimumTradeCount())
                .sorted(Comparator.comparingInt(Candidate::dealCount).reversed())
                .limit(query.limit())
                .toList();

        List<TradeVolumeRankingResponse.Item> items = java.util.stream.IntStream.range(0, candidates.size())
                .mapToObj(index -> candidates.get(index).toItem(index + 1))
                .toList();
        int totalDealCount = responses.stream()
                .mapToInt(item -> item.response().totalDealCnt() == null ? 0 : item.response().totalDealCnt())
                .sum();

        String periodStart = candidates.isEmpty() ? null : candidates.get(0).periodStart();
        String periodEnd = candidates.isEmpty() ? null : candidates.get(0).periodEnd();
        return new TradeVolumeRankingResponse("서울 전체", periodStart, periodEnd, totalDealCount,
                criteria(periodStart, periodEnd, query.minimumTradeCount()), items);
    }

    private List<Candidate> candidates(Region region, RttRespopnse response) {
        if (response.top5ByVolume() == null) return List.of();
        return response.top5ByVolume().stream()
                .map(item -> new Candidate(region.name(), response.periodStart(), response.periodEnd(),
                        item.aptName(), item.mno(), item.sno(), item.dealCnt() == null ? 0 : item.dealCnt(),
                        item.avgTradeAmount()))
                .toList();
    }

    private TradeVolumeRankingResponse toResponse(String regionName, RttRespopnse response, RankingSearchQuery query) {
        List<TradeVolumeRankingResponse.Item> items = response.top5ByVolume() == null ? List.of()
                : response.top5ByVolume().stream()
                .filter(item -> item.dealCnt() != null && item.dealCnt() >= query.minimumTradeCount())
                .limit(query.limit())
                .map(item -> new TradeVolumeRankingResponse.Item(
                        0, regionName, item.aptName(), item.mno(), item.sno(),
                        item.dealCnt() == null ? 0 : item.dealCnt(), item.avgTradeAmount()))
                .toList();

        List<TradeVolumeRankingResponse.Item> ranked = java.util.stream.IntStream.range(0, items.size())
                .mapToObj(index -> {
                    TradeVolumeRankingResponse.Item item = items.get(index);
                    return new TradeVolumeRankingResponse.Item(index + 1, item.regionName(), item.apartmentName(),
                            item.mainAddressNumber(), item.subAddressNumber(), item.dealCount(),
                            item.averageTradeAmount());
                })
                .toList();

        return new TradeVolumeRankingResponse(regionName, response.periodStart(), response.periodEnd(),
                response.totalDealCnt() == null ? 0 : response.totalDealCnt(),
                criteria(response.periodStart(), response.periodEnd(), query.minimumTradeCount()), ranked);
    }

    private RankingCriteria criteria(String periodStart, String periodEnd, int minimumTradeCount) {
        String period = periodStart == null || periodEnd == null
                ? "최근 집계 기간" : periodStart + " ~ " + periodEnd;
        return new RankingCriteria("거래 건수", "건", period, minimumTradeCount, "높은 순");
    }

    private Region resolveRegionLegacy(String question) {
        Matcher fullRegion = RegionQuestionPatterns.FULL_REGION.matcher(question);
        if (fullRegion.find()) {
            SggMaster sgg = findSgg(fullRegion.group(1));
            String dongName = fullRegion.group(2);
            DongRegionResponse dong = locationService.resolveDong(dongName).stream()
                    .filter(candidate -> candidate.sggCode().equals(sgg.getSggCode()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("자치구와 자치동 조합을 찾지 못했습니다."));
            return new Region(sgg.getSggCode(), dong.dongCode(), sgg.getSggName() + " " + dong.dongName());
        }

        Matcher district = RegionQuestionPatterns.DISTRICT.matcher(question);
        if (district.find()) {
            SggMaster sgg = findSgg(district.group(1));
            return new Region(sgg.getSggCode(), null, sgg.getSggName());
        }
        return null;
    }

    private SggMaster findSgg(String sggName) {
        return sggRepository.findBySggName(sggName)
                .orElseThrow(() -> new IllegalArgumentException(sggName + "을 찾지 못했습니다."));
    }

    private Region resolveRegion(String question) {
        RankingRegionResolver.ResolvedRegion resolved = regionResolver.resolve(question);
        if (resolved.allSeoul()) return null;
        return new Region(resolved.sggCode(), resolved.dongCode(), resolved.name());
    }

    private record Region(String sggCode, String dongCode, String name) {}

    private record RegionResponse(Region region, RttRespopnse response) {}

    private record Candidate(String regionName, String periodStart, String periodEnd, String apartmentName,
                             String mainAddressNumber, String subAddressNumber, int dealCount,
                             Long averageTradeAmount) {
        private TradeVolumeRankingResponse.Item toItem(int rank) {
            return new TradeVolumeRankingResponse.Item(rank, regionName, apartmentName, mainAddressNumber,
                    subAddressNumber, dealCount, averageTradeAmount);
        }
    }
}
