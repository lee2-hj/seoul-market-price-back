package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.NearbyApartmentRequest;
import com.seoul.market.seoulmarketprice.ai.dto.NearbyApartmentResponse;
import com.seoul.market.seoulmarketprice.ai.dto.PlaceResolutionResponse;
import com.seoul.market.seoulmarketprice.ai.dto.PriceComparisonResponse;
import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class NearestApartmentPriceSearchService {
    private final PlaceResolver placeResolver;
    private final NearbyApartmentSearchService nearbyApartmentSearchService;

    public NearestApartmentPriceSearchService(PlaceResolver placeResolver,
                                               NearbyApartmentSearchService nearbyApartmentSearchService) {
        this.placeResolver = placeResolver;
        this.nearbyApartmentSearchService = nearbyApartmentSearchService;
    }

    public PriceComparisonResponse search(QuestionAnalysisResponse analysis) {
        QuestionAnalysisResponse.AnalyzedPlace reference = analysis.referencePlace();
        if (reference == null || reference.name() == null || reference.name().isBlank()) {
            throw new IllegalArgumentException("기준 장소를 찾을 수 없습니다. 역이나 장소 이름을 포함해주세요.");
        }
        PlaceResolutionResponse resolution = placeResolver.resolve(reference.name(), reference.type());
        PlaceResolutionResponse.PlaceCandidate place = representativePlace(resolution, reference.name());
        int limit = analysis.limit() == null ? 1 : Math.max(1, Math.min(analysis.limit(), 10));
        NearbyApartmentResponse nearby = nearbyApartmentSearchService.search(
                new NearbyApartmentRequest(place.latitude(), place.longitude(), null, limit));
        if (!"SUCCESS".equals(nearby.status()) || nearby.apartments().isEmpty()) {
            throw new IllegalArgumentException(nearby.message() == null
                    ? "기준 장소 주변의 아파트를 찾을 수 없습니다." : nearby.message());
        }
        NearbyApartmentResponse.ApartmentCandidate apartment = nearby.apartments().getFirst();
        if (apartment.averageTradeAmount() == null) {
            throw new IllegalArgumentException("가장 가까운 아파트는 찾았지만 가격 데이터가 없습니다.");
        }
        String amount = NumberFormat.getIntegerInstance(Locale.KOREA).format(apartment.averageTradeAmount());
        List<String> points = new ArrayList<>();
        points.add("기준 장소: " + place.name());
        points.add("거리: 약 " + apartment.distanceMeters() + "m");
        points.add("주소: " + apartment.address());
        if (apartment.averagePyeongAmount() != null) {
            points.add("평균 평단가: " + NumberFormat.getIntegerInstance(Locale.KOREA)
                    .format(apartment.averagePyeongAmount()) + "만원/평");
        }
        List<String> cautions = new ArrayList<>();
        cautions.add("평균 거래가 · 만원 · 조회 데이터 기준");
        if (apartment.dealCount() != null) cautions.add("집계 거래 " + apartment.dealCount() + "건");
        if (apartment.latestDealDate() != null) cautions.add("최근 거래일: " + apartment.latestDealDate());
        if (apartment.baseDate() != null) cautions.add("데이터 기준일: " + apartment.baseDate());
        return new PriceComparisonResponse(
                reference.name() + "에서 가장 가까운 아파트는 " + apartment.apartmentName()
                        + "이며, 조회 데이터 기준 평균 거래가는 " + amount + "만원입니다.",
                points, cautions);
    }

    private PlaceResolutionResponse.PlaceCandidate representativePlace(PlaceResolutionResponse resolution,
                                                                         String requestedName) {
        if (resolution.resolvedPlace() != null) return resolution.resolvedPlace();
        if (resolution.candidates() != null && !resolution.candidates().isEmpty()) {
            return resolution.candidates().getFirst();
        }
        throw new IllegalArgumentException(resolution.message() == null
                ? requestedName + "을(를) 찾을 수 없습니다." : resolution.message());
    }
}
