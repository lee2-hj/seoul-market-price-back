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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** 서울시 전체 자치구의 실거래 데이터를 거래 건수 기준으로 가중 집계한다. */
@Service
public class CitySummarySearchService {
    private static final Logger log = LoggerFactory.getLogger(CitySummarySearchService.class);
    private final SggMasterRepository sggRepository;
    private final FastApiService fastApiService;
    private final RestClient aiClient;

    public CitySummarySearchService(SggMasterRepository sggRepository, FastApiService fastApiService,
                                    @Qualifier("aiFastApiRestClient") RestClient aiClient) {
        this.sggRepository = sggRepository;
        this.fastApiService = fastApiService;
        this.aiClient = aiClient;
    }

    public SingleRegionPriceResponse search(String question) {
        List<SggMaster> districts = sggRepository.findAllByOrderBySggNameAsc();
        if (districts.isEmpty()) {
            throw new IllegalArgumentException("서울시 자치구 정보를 찾을 수 없습니다.");
        }

        long priceWeightedSum = 0;
        long priceCount = 0;
        long pyeongWeightedSum = 0;
        long pyeongCount = 0;
        long totalCount = 0;
        String baseDate = null;
        int includedDistrictCount = 0;

        for (SggMaster district : districts) {
            ListResponse response = fastApiService.getPyeongList(new ListRequest(district.getSggCode()));
            if (response == null || response.groups() == null || response.groups().isEmpty()) continue;

            boolean districtIncluded = false;
            for (ListResponse.ListSummaryDto item : response.groups().values()) {
                long count = item.total_count() == null ? 0 : item.total_count();
                if (count <= 0) continue;

                totalCount += count;
                districtIncluded = true;
                if (item.avg_thing_amt() != null) {
                    priceWeightedSum += item.avg_thing_amt() * count;
                    priceCount += count;
                }
                if (item.avg_pyeong_amt() != null) {
                    pyeongWeightedSum += item.avg_pyeong_amt() * count;
                    pyeongCount += count;
                }
            }
            if (districtIncluded) includedDistrictCount++;
            if (response.baseDate() != null
                    && (baseDate == null || response.baseDate().compareTo(baseDate) > 0)) {
                baseDate = response.baseDate();
            }
        }

        if (totalCount == 0 || priceCount == 0) {
            throw new IllegalArgumentException("서울시 전체 가격 데이터가 없습니다.");
        }

        long averagePrice = Math.round((double) priceWeightedSum / priceCount);
        long averagePyeongPrice = pyeongCount == 0
                ? 0 : Math.round((double) pyeongWeightedSum / pyeongCount);
        SingleRegionFacts facts = new SingleRegionFacts("서울시 전체", averagePrice, averagePyeongPrice,
                Math.toIntExact(totalCount), baseDate, "만원", "만원/평");
        SingleRegionPriceRequest request = new SingleRegionPriceRequest("city-summary-search", question, facts,
                List.of("서울시 " + includedDistrictCount + "개 자치구의 거래 건수 가중평균 가격 설명",
                        "전체 거래 건수와 데이터 기준일 표시"),
                List.of("가격 예측", "투자 추천", "자치구 평균의 단순 산술평균"));
        try {
            SingleRegionPriceResponse response = aiClient.post().uri("/ai/explain-single-region").body(request).retrieve()
                    .body(SingleRegionPriceResponse.class);
            return response == null ? fallbackResponse(facts) : response;
        } catch (RuntimeException exception) {
            log.warn("City summary explanation unavailable; returning factual fallback", exception);
            return fallbackResponse(facts);
        }
    }

    private SingleRegionPriceResponse fallbackResponse(SingleRegionFacts facts) {
        String summary = facts.region() + " 평균 거래가는 " + money(facts.averagePrice()) + "입니다.";
        List<String> keyPoints = List.of(
                "평균 평단가: " + number(facts.averagePyeongPrice()) + facts.averagePyeongPriceUnit(),
                "거래 건수: " + number(facts.transactionCount()) + "건");
        List<String> cautions = List.of(
                facts.baseDate() == null ? "기준일 정보 없음" : "기준일: " + facts.baseDate(),
                "자치구별 거래 건수를 가중치로 계산한 평균입니다.");
        return new SingleRegionPriceResponse(summary, keyPoints, cautions);
    }

    private String money(long valueInManwon) {
        if (valueInManwon < 10_000L) return number(valueInManwon) + "만원";
        long eok = valueInManwon / 10_000L;
        long remainder = valueInManwon % 10_000L;
        return remainder == 0 ? number(eok) + "억원" : number(eok) + "억 " + number(remainder) + "만원";
    }

    private String number(long value) {
        return String.format("%,d", value);
    }
}
