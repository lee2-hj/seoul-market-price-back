package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.SingleRegionFacts;
import com.seoul.market.seoulmarketprice.ai.dto.SingleRegionPriceRequest;
import com.seoul.market.seoulmarketprice.ai.dto.SingleRegionPriceResponse;
import com.seoul.market.seoulmarketprice.fastapi.dto.request.ListRequest;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.ListResponse;
import com.seoul.market.seoulmarketprice.fastapi.service.FastApiService;
import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DistrictSummarySearchService {
    private static final Pattern GU_PATTERN = Pattern.compile("([가-힣]+구)");
    private final SggMasterRepository sggRepository;
    private final FastApiService fastApiService;
    private final RestClient aiClient;

    public DistrictSummarySearchService(SggMasterRepository sggRepository, FastApiService fastApiService,
                                        @Qualifier("aiFastApiRestClient") RestClient aiClient) {
        this.sggRepository = sggRepository;
        this.fastApiService = fastApiService;
        this.aiClient = aiClient;
    }

    public SingleRegionPriceResponse search(String question) {
        Matcher matcher = GU_PATTERN.matcher(question);
        if (!matcher.find()) throw new IllegalArgumentException("서울시 자치구를 입력해주세요.");
        String guName = matcher.group(1);
        SggMaster gu = sggRepository.findBySggName(guName)
                .orElseThrow(() -> new IllegalArgumentException(guName + "을(를) 찾을 수 없습니다."));
        ListResponse list = fastApiService.getPyeongList(new ListRequest(gu.getSggCode()));

        long totalCount = list.groups().values().stream()
                .mapToLong(item -> item.total_count() == null ? 0 : item.total_count())
                .sum();
        if (totalCount == 0) throw new IllegalArgumentException(guName + "의 가격 데이터가 없습니다.");

        long averagePrice = weightedAverage(list, false);
        long averagePyeongPrice = weightedAverage(list, true);
        SingleRegionFacts facts = new SingleRegionFacts(guName, averagePrice, averagePyeongPrice,
                Math.toIntExact(totalCount), list.baseDate(), "만원", "만원/평");
        SingleRegionPriceRequest request = new SingleRegionPriceRequest("district-summary-search", question, facts,
                List.of(guName + "의 조회 데이터 기준 평균 가격 설명"), List.of("가격 예측", "투자 추천"));
        return aiClient.post().uri("/ai/explain-single-region").body(request).retrieve()
                .body(SingleRegionPriceResponse.class);
    }

    private long weightedAverage(ListResponse list, boolean pyeong) {
        long weightedSum = 0;
        long countSum = 0;
        for (ListResponse.ListSummaryDto item : list.groups().values()) {
            Long value = pyeong ? item.avg_pyeong_amt() : item.avg_thing_amt();
            long count = item.total_count() == null ? 0 : item.total_count();
            if (value != null && count > 0) {
                weightedSum += value * count;
                countSum += count;
            }
        }
        if (countSum == 0) return 0;
        return Math.round((double) weightedSum / countSum);
    }
}
