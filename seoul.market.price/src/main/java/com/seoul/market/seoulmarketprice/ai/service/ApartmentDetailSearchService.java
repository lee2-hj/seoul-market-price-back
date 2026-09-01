package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.PriceComparisonResponse;
import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import com.seoul.market.seoulmarketprice.elasticSearch.dto.request.AptNameRequest;
import com.seoul.market.seoulmarketprice.elasticSearch.dto.response.AptNameResponse;
import com.seoul.market.seoulmarketprice.elasticSearch.service.ElasticSearchService;
import com.seoul.market.seoulmarketprice.fastapi.dto.request.AptMktRequest;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.AptMktResponse;
import com.seoul.market.seoulmarketprice.fastapi.service.FastApiService;
import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocationRepository;
import com.seoul.market.seoulmarketprice.ai.dto.NaturalApartmentCandidate;
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
    private final ApartmentLocationRepository apartmentLocationRepository;

    public ApartmentDetailSearchService(ElasticSearchService elasticSearchService,
                                        FastApiService fastApiService,
                                        ApartmentLocationRepository apartmentLocationRepository) {
        this.elasticSearchService = elasticSearchService;
        this.fastApiService = fastApiService;
        this.apartmentLocationRepository = apartmentLocationRepository;
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
        // ES may lag behind the latest Parquet partition. Search Parquet too, then relax location filters progressively.
        List<AptNameResponse> parquetExact = apartmentLocationRepository.findByApartmentNameLike(apartmentName).stream()
                .map(item -> new AptNameResponse(item.apartmentName(), item.mainNumber(), item.subNumber(),
                        item.dongCode(), item.dongName(), item.sggCode(), item.districtName()))
                .filter(item -> normalized(item.apt_name()).equals(normalized(apartmentName)))
                .toList();
        List<AptNameResponse> stage1 = parquetExact.stream()
                .filter(item -> requestedDistrict == null || requestedDistrict.equals(item.sgg_nm()))
                .filter(item -> requestedDong == null || requestedDong.equals(item.dong_nm())).toList();
        if (stage1.stream().map(item -> item.sgg_cd() + item.dong_cd() + item.mno() + item.sno()).distinct().count() == 1) return stage1.getFirst();
        List<AptNameResponse> stage2 = parquetExact.stream()
                .filter(item -> requestedDistrict == null || requestedDistrict.equals(item.sgg_nm())).toList();
        if (stage2.stream().map(item -> item.sgg_cd() + item.dong_cd() + item.mno() + item.sno()).distinct().count() == 1) return stage2.getFirst();
        if (!stage2.isEmpty()) throw selectionRequired(apartmentName, stage2);
        if (parquetExact.stream().map(item -> item.sgg_cd() + item.dong_cd() + item.mno() + item.sno()).distinct().count() == 1
                && !parquetExact.isEmpty()) return parquetExact.getFirst();
        if (!parquetExact.isEmpty()) throw selectionRequired(apartmentName, parquetExact);
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

    private ApartmentSelectionRequiredException selectionRequired(String apartmentName, List<AptNameResponse> values) {
        List<NaturalApartmentCandidate> candidates = values.stream()
                .collect(java.util.stream.Collectors.toMap(item -> item.sgg_cd() + item.dong_cd() + item.mno() + item.sno(), item -> item,
                        (left, right) -> left, java.util.LinkedHashMap::new)).values().stream().limit(5)
                .map(item -> new NaturalApartmentCandidate(item.apt_name(), item.sgg_nm(), item.sgg_cd(), item.dong_nm(),
                        item.dong_cd(), item.mno(), item.sno())).toList();
        return new ApartmentSelectionRequiredException(apartmentName + "과(와) 일치하는 단지가 여러 곳입니다. 단지를 선택해주세요.", candidates);
    }

    private String normalized(String value) {
        return value == null ? "" : value.replaceAll("(?i)아파트|단지", "")
                .replaceAll("제\\s*(\\d+)\\s*차", "$1차")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "").toLowerCase(Locale.ROOT);
    }

    private String money(Long value) {
        return value == null ? "정보 없음" : NumberFormat.getNumberInstance(Locale.KOREA).format(value) + "만원";
    }

    private String number(Integer value) {
        return value == null ? "0" : NumberFormat.getNumberInstance(Locale.KOREA).format(value);
    }
}
