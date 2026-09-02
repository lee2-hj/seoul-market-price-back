package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.NearbyApartmentRequest;
import com.seoul.market.seoulmarketprice.ai.dto.NearbyApartmentResponse;
import com.seoul.market.seoulmarketprice.ai.dto.PlaceResolutionResponse;
import com.seoul.market.seoulmarketprice.ai.dto.PriceRankingResponse;
import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import com.seoul.market.seoulmarketprice.ai.dto.RankingCriteria;
import com.seoul.market.seoulmarketprice.ai.query.GenericQueryExecutor;
import com.seoul.market.seoulmarketprice.ai.query.MetricRecord;
import com.seoul.market.seoulmarketprice.ai.query.QueryRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/** 장소(역·랜드마크) 좌표를 기준으로 주변 아파트를 평균 거래가 순으로 조회한다. */
@Service
public class NearbyApartmentRankingSearchService {
    private static final int PRIMARY_RADIUS_METERS = 1_000;
    private static final int FALLBACK_RADIUS_METERS = 3_000;
    private static final int CANDIDATE_LIMIT = 50;
    private static final int MINIMUM_TRADE_COUNT = 3;
    private static final int DEFAULT_LIMIT = 5;

    private final PlaceResolver placeResolver;
    private final NearbyApartmentSearchService nearbyApartmentSearchService;
    private final GenericQueryExecutor queryExecutor;

    @Autowired
    public NearbyApartmentRankingSearchService(PlaceResolver placeResolver,
                                                NearbyApartmentSearchService nearbyApartmentSearchService,
                                                GenericQueryExecutor queryExecutor) {
        this.placeResolver = placeResolver;
        this.nearbyApartmentSearchService = nearbyApartmentSearchService;
        this.queryExecutor = queryExecutor;
    }

    NearbyApartmentRankingSearchService(PlaceResolver placeResolver,
                                        NearbyApartmentSearchService nearbyApartmentSearchService) {
        this(placeResolver, nearbyApartmentSearchService, new GenericQueryExecutor());
    }

    public PriceRankingResponse search(QuestionAnalysisResponse analysis) {
        QuestionAnalysisResponse.AnalyzedPlace reference = analysis.referencePlace();
        if (reference == null || reference.name() == null || reference.name().isBlank()) {
            throw new IllegalArgumentException("기준 장소를 찾을 수 없습니다. 역 또는 장소 이름을 포함해주세요.");
        }

        PlaceResolutionResponse resolution = placeResolver.resolve(reference.name(), reference.type());
        PlaceResolutionResponse.PlaceCandidate place = representativePlace(resolution, reference.name());
        int limit = analysis.limit() == null ? DEFAULT_LIMIT : Math.max(1, Math.min(analysis.limit(), 10));

        NearbyApartmentResponse nearby = searchRankableApartments(place, PRIMARY_RADIUS_METERS);
        if (!hasRankableApartment(nearby, analysis.filters())) {
            nearby = searchRankableApartments(place, FALLBACK_RADIUS_METERS);
        }
        if (!"SUCCESS".equals(nearby.status())) {
            throw new IllegalArgumentException(nearby.message() == null
                    ? "기준 장소 주변의 아파트를 찾을 수 없습니다." : nearby.message());
        }

        List<NearbyApartmentResponse.ApartmentCandidate> ranked = matchingApartments(nearby.apartments(),
                analysis.filters(), limit);
        if (ranked.isEmpty()) {
            if (hasRequestedFilters(analysis.filters())) {
                throw new IllegalArgumentException("기준 장소 주변에서 요청한 가격·면적 조건을 만족하는 아파트 거래 데이터를 찾을 수 없습니다.");
            }
            throw new IllegalArgumentException("기준 장소 주변에서 거래 3건 이상인 아파트 가격 데이터를 찾을 수 없습니다.");
        }

        String radiusLabel = formatRadius(nearby.radiusMeters());
        String baseDate = ranked.stream().map(NearbyApartmentResponse.ApartmentCandidate::baseDate)
                .filter(value -> value != null && !value.isBlank()).findFirst().orElse(null);
        String period = (baseDate == null ? "조회 데이터 기준" : baseDate + " 기준") + " · 반경 " + radiusLabel;
        List<PriceRankingResponse.Item> items = java.util.stream.IntStream.range(0, ranked.size())
                .mapToObj(index -> {
                    NearbyApartmentResponse.ApartmentCandidate apartment = ranked.get(index);
                    return new PriceRankingResponse.Item(index + 1, apartment.address(), apartment.apartmentName(),
                            apartment.averageTradeAmount(), apartment.dealCount());
                }).toList();

        return new PriceRankingResponse(place.name() + " 주변 " + radiusLabel,
                "AVERAGE_TRADE_AMOUNT", baseDate,
                new RankingCriteria("평균 거래가", "만원", period, MINIMUM_TRADE_COUNT, "높은 순"), items);
    }

    private NearbyApartmentResponse searchRankableApartments(PlaceResolutionResponse.PlaceCandidate place, int radius) {
        return nearbyApartmentSearchService.search(new NearbyApartmentRequest(
                place.latitude(), place.longitude(), radius, CANDIDATE_LIMIT));
    }

    private boolean hasRankableApartment(NearbyApartmentResponse response,
                                         QuestionAnalysisResponse.SearchFilters filters) {
        return "SUCCESS".equals(response.status()) && response.apartments().stream()
                .anyMatch(apartment -> !matchingApartments(List.of(apartment), filters, 1).isEmpty());
    }

    private List<NearbyApartmentResponse.ApartmentCandidate> matchingApartments(
            List<NearbyApartmentResponse.ApartmentCandidate> apartments,
            QuestionAnalysisResponse.SearchFilters filters, int limit) {
        java.util.Map<String, NearbyApartmentResponse.ApartmentCandidate> candidatesById = new java.util.HashMap<>();
        List<MetricRecord> records = apartments.stream().map(apartment -> toMetricRecord(apartment, candidatesById))
                .toList();
        List<MetricRecord> matched = queryExecutor.execute(records,
                new QueryRequest((long) MINIMUM_TRADE_COUNT,
                        filters == null ? null : filters.minPyeong(),
                        filters == null ? null : filters.maxPyeong(),
                        filters == null ? null : filters.minPriceWon(),
                        exclusiveUpperBound(filters == null ? null : filters.maxPriceWon()),
                        QueryRequest.SortField.AVERAGE_PRICE, false, limit));
        return matched.stream().map(record -> candidatesById.get(record.sourceId())).toList();
    }

    private MetricRecord toMetricRecord(NearbyApartmentResponse.ApartmentCandidate apartment,
                                        java.util.Map<String, NearbyApartmentResponse.ApartmentCandidate> candidatesById) {
        String sourceId = apartment.apartmentId() + "|" + apartment.exclusiveAreaM2() + "|"
                + apartment.averageTradeAmount() + "|" + apartment.latestDealDate();
        candidatesById.put(sourceId, apartment);
        Double pyeong = apartment.exclusiveAreaM2() == null ? null : apartment.exclusiveAreaM2() / 3.305785D;
        return new MetricRecord(sourceId, null, null, apartment.apartmentName(), apartment.address(),
                apartment.averageTradeAmount() == null ? null : apartment.averageTradeAmount() * 10_000L,
                apartment.averagePyeongAmount(), apartment.exclusiveAreaM2(), pyeong,
                apartment.dealCount() == null ? null : apartment.dealCount().longValue(),
                apartment.latestDealDate(), apartment.baseDate());
    }

    private Long exclusiveUpperBound(Long value) {
        return value == null ? null : value - 1;
    }

    private boolean hasRequestedFilters(QuestionAnalysisResponse.SearchFilters filters) {
        return filters != null && (filters.minPriceWon() != null || filters.maxPriceWon() != null
                || filters.minPyeong() != null || filters.maxPyeong() != null);
    }

    private PlaceResolutionResponse.PlaceCandidate representativePlace(PlaceResolutionResponse resolution,
                                                                         String requestedName) {
        if (resolution.resolvedPlace() != null) return resolution.resolvedPlace();
        if (resolution.candidates() != null && !resolution.candidates().isEmpty()) return resolution.candidates().getFirst();
        throw new IllegalArgumentException(resolution.message() == null
                ? requestedName + "을(를) 찾을 수 없습니다." : resolution.message());
    }

    private String formatRadius(int radiusMeters) {
        return radiusMeters % 1_000 == 0 ? radiusMeters / 1_000 + "km" : radiusMeters + "m";
    }
}
