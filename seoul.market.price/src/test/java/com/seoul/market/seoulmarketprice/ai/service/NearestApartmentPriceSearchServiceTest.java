package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NearestApartmentPriceSearchServiceTest {

    @Test
    void returnsNearestApartmentPriceInDisplayFormat() {
        PlaceResolver placeResolver = mock(PlaceResolver.class);
        NearbyApartmentSearchService nearbyService = mock(NearbyApartmentSearchService.class);
        var place = new PlaceResolutionResponse.PlaceCandidate("1", "홍대입구역 2호선", "STATION",
                "서울 마포구 동교동", "", 37.5568, 126.9237, "KAKAO");
        when(placeResolver.resolve("홍대입구", "STATION"))
                .thenReturn(PlaceResolutionResponse.clarification(List.of(place)));
        var apartment = new NearbyApartmentResponse.ApartmentCandidate("a1", "테스트아파트",
                "마포구 동교동 1", "11440", "10100", 37.557, 126.924, 120,
                123456L, 5678L, 3, "2026-08-20", "2026-08-24");
        when(nearbyService.search(any())).thenReturn(new NearbyApartmentResponse(
                "SUCCESS", null, 500, "s3://warehouse/mart/dm_main/", List.of(apartment)));
        NearestApartmentPriceSearchService service =
                new NearestApartmentPriceSearchService(placeResolver, nearbyService);

        PriceComparisonResponse response = service.search(analysis());

        assertThat(response.summary()).contains("테스트아파트", "123,456만원");
        assertThat(response.keyPoints()).contains("거리: 약 120m", "평균 평단가: 5,678만원/평");
        assertThat(response.cautions()).contains("집계 거래 3건", "데이터 기준일: 2026-08-24");
    }

    private QuestionAnalysisResponse analysis() {
        return new QuestionAnalysisResponse("NEAREST_APARTMENT_PRICE", List.of(),
                new QuestionAnalysisResponse.AnalyzedPlace("홍대입구", "STATION"),
                "APARTMENT", null, null, 1, null, List.of("LATEST_PRICE"),
                List.of("RESOLVE_PLACE", "SEARCH_NEARBY_APARTMENTS", "GET_APARTMENT_PRICE"), List.of());
    }
}
