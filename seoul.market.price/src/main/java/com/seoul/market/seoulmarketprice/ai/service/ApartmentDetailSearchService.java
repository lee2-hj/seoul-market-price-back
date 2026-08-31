package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.PriceComparisonResponse;
import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import com.seoul.market.seoulmarketprice.elasticSearch.dto.request.AptNameRequest;
import com.seoul.market.seoulmarketprice.elasticSearch.dto.response.AptNameResponse;
import com.seoul.market.seoulmarketprice.elasticSearch.service.ElasticSearchService;
import com.seoul.market.seoulmarketprice.fastapi.dto.request.AptMktRequest;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.AptMktResponse;
import com.seoul.market.seoulmarketprice.fastapi.service.FastApiService;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Natural-language lookup of one apartment complex using the existing Elasticsearch and market APIs. */
@Service
public class ApartmentDetailSearchService {
    private final ElasticSearchService elasticSearchService;
    private final FastApiService fastApiService;

    public ApartmentDetailSearchService(ElasticSearchService elasticSearchService,
                                        FastApiService fastApiService) {
        this.elasticSearchService = elasticSearchService;
        this.fastApiService = fastApiService;
    }

    public PriceComparisonResponse search(QuestionAnalysisResponse analysis) {
        if (analysis.apartmentName() == null || analysis.apartmentName().isBlank()) {
            throw new IllegalArgumentException("조회할 아파트 단지명을 찾지 못했습니다. 자치구, 동, 단지명을 함께 입력해주세요.");
        }
        AptNameResponse apartment = resolveApartment(analysis);
        AptMktResponse market = fastApiService.getAptmktInfo(new AptMktRequest(
                apartment.sgg_cd(), apartment.dong_cd(), apartment.apt_name(), apartment.mno(), apartment.sno()));
        AptMktResponse.AptMktDataDto data = market.data() == null ? null : market.data().stream()
                .filter(item -> normalized(item.aptName()).equals(normalized(apartment.apt_name())))
                .findFirst().orElseGet(() -> market.data().isEmpty() ? null : market.data().getFirst());
        if (data == null) {
            throw new IllegalArgumentException("해당 단지의 최근 거래 데이터를 찾을 수 없습니다.");
        }

        String period = market.searchPeriod() == null ? "최근 집계 기간" : market.searchPeriod().startDate()
                + " ~ " + market.searchPeriod().endDate();
        List<String> keyPoints = new java.util.ArrayList<>();
        keyPoints.add("평균 거래가: " + money(data.averageDealPrice()));
        keyPoints.add("거래 건수: " + number(data.totalDealCount()) + "건");
        if (data.maxDealPrice() != null) keyPoints.add("최고 거래가: " + money(data.maxDealPrice()));
        if (data.recentDeals() != null && !data.recentDeals().isEmpty()) {
            AptMktResponse.RecentDealDto recent = data.recentDeals().stream()
                    .max(Comparator.comparing(AptMktResponse.RecentDealDto::dealDate,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);
            if (recent != null) keyPoints.add("최근 거래: " + recent.dealDate() + " · " + money(recent.dealAmount()));
        }
        return new PriceComparisonResponse(
                (data.cggNm() == null ? apartment.sgg_nm() : data.cggNm()) + " "
                        + (data.stdgNm() == null ? apartment.dong_nm() : data.stdgNm()) + " "
                        + apartment.apt_name() + "의 조회 데이터입니다.",
                keyPoints,
                List.of("조회 기간: " + period, "단지별 최근 집계 거래 데이터를 기준으로 합니다."));
    }

    private AptNameResponse resolveApartment(QuestionAnalysisResponse analysis) {
        String apartmentName = analysis.apartmentName();
        String requestedDistrict = analysis.regions().stream().filter(region -> "DISTRICT".equals(region.type()))
                .map(QuestionAnalysisResponse.AnalyzedRegion::name).findFirst().orElse(null);
        String requestedDong = analysis.regions().stream().filter(region -> "DONG".equals(region.type()))
                .map(QuestionAnalysisResponse.AnalyzedRegion::name).findFirst().orElse(null);
        List<AptNameResponse> exactMatches = elasticSearchService.searchAptName(
                        new AptNameRequest(apartmentName, null, null)).stream()
                .filter(item -> normalized(item.apt_name()).equals(normalized(apartmentName)))
                .filter(item -> requestedDistrict == null || requestedDistrict.equals(item.sgg_nm()))
                .filter(item -> requestedDong == null || requestedDong.equals(item.dong_nm()))
                .toList();
        if (exactMatches.isEmpty()) {
            throw new IllegalArgumentException(apartmentName + " 단지를 찾을 수 없습니다. 단지명 또는 지역을 다시 확인해주세요.");
        }
        long distinctLocations = exactMatches.stream().map(item -> item.sgg_cd() + "-" + item.dong_cd()
                + "-" + item.mno() + "-" + item.sno()).distinct().count();
        if (distinctLocations > 1) {
            throw new IllegalArgumentException("같은 이름의 단지가 여러 곳에 있습니다. 자치구 또는 동을 함께 입력해주세요.");
        }
        return exactMatches.getFirst();
    }

    private String normalized(String value) {
        return value == null ? "" : value.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "").toLowerCase(Locale.ROOT);
    }

    private String money(Long value) {
        return value == null ? "정보 없음" : NumberFormat.getNumberInstance(Locale.KOREA).format(value) + "만원";
    }

    private String number(Integer value) {
        return value == null ? "0" : NumberFormat.getNumberInstance(Locale.KOREA).format(value);
    }
}
