package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.NearbyApartmentResponse;
import com.seoul.market.seoulmarketprice.ai.dto.PlaceResolutionResponse;
import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NearbyApartmentRankingSearchServiceTest {
    @Test
    void ranksApartmentsNearResolvedPlaceByAverageTradeAmount() {
        PlaceResolver placeResolver = mock(PlaceResolver.class);
        NearbyApartmentSearchService nearbySearch = mock(NearbyApartmentSearchService.class);
        var place = new PlaceResolutionResponse.PlaceCandidate("1", "홍대입구역", "STATION", null, null,
                37.5572, 126.9236, "KAKAO");
        when(placeResolver.resolve("홍대입구", "STATION")).thenReturn(PlaceResolutionResponse.resolved(place));
        when(nearbySearch.search(any())).thenReturn(new NearbyApartmentResponse("SUCCESS", null, 1000, "minio",
                List.of(candidate("낮은가격", 100_000L, 10), candidate("거래부족", 500_000L, 2),
                        candidate("가장비��", 300_000L, 3), candidate("중간가격", 200_000L, 4))));

        var service = new NearbyApartmentRankingSearchService(placeResolver, nearbySearch);
        var result = service.search(analysis(5));

        assertThat(result.regionName()).isEqualTo("홍대입구역 주변 1km");
        assertThat(result.criteria().metric()).isEqualTo("평균 거래가");
        assertThat(result.criteria().period()).contains("반경 1km");
        assertThat(result.items()).extracting(item -> item.apartmentName())
                .containsExactly("가장비��", "중간가격", "낮은가격");
        assertThat(result.items()).extracting(item -> item.metricValue())
                .containsExactly(300_000L, 200_000L, 100_000L);
    }

    @Test
    void expandsRadiusWhenPrimaryRadiusHasNoRankableApartment() {
        PlaceResolver placeResolver = mock(PlaceResolver.class);
        NearbyApartmentSearchService nearbySearch = mock(NearbyApartmentSearchService.class);
        var place = new PlaceResolutionResponse.PlaceCandidate("1", "홍대입구역", "STATION", null, null,
                37.5572, 126.9236, "KAKAO");
        when(placeResolver.resolve("홍대입구", "STATION")).thenReturn(PlaceResolutionResponse.resolved(place));
        when(nearbySearch.search(any()))
                .thenReturn(new NearbyApartmentResponse("SUCCESS", null, 1000, "minio", List.of(candidate("거래부족", 500_000L, 2))))
                .thenReturn(new NearbyApartmentResponse("SUCCESS", null, 3000, "minio", List.of(candidate("대상", 250_000L, 3))));

        var result = new NearbyApartmentRankingSearchService(placeResolver, nearbySearch).search(analysis(5));

        assertThat(result.regionName()).isEqualTo("홍대입구역 주변 3km");
        assertThat(result.items()).hasSize(1);
    }

    @Test
    void appliesPriceBandBeforeRankingNearbyApartments() {
        PlaceResolver placeResolver = mock(PlaceResolver.class);
        NearbyApartmentSearchService nearbySearch = mock(NearbyApartmentSearchService.class);
        var place = new PlaceResolutionResponse.PlaceCandidate("1", "강동역", "STATION", null, null,
                37.535, 127.133, "KAKAO");
        when(placeResolver.resolve("강동역", "STATION")).thenReturn(PlaceResolutionResponse.resolved(place));
        when(nearbySearch.search(any())).thenReturn(new NearbyApartmentResponse("SUCCESS", null, 1000, "minio",
                List.of(candidate("four-eok", 41_678L, 3), candidate("twenty-eok", 205_000L, 4),
                        candidate("twenty-one-eok", 210_000L, 5))));

        var filters = new QuestionAnalysisResponse.SearchFilters(null, null, 2_000_000_000L, 2_100_000_000L);
        var analysis = new QuestionAnalysisResponse("NEARBY_APARTMENT_RANKING", List.of(),
                new QuestionAnalysisResponse.AnalyzedPlace("강동역", "STATION"), "APARTMENT",
                null, "AVERAGE_PRICE", "DESC", 5, null, List.of(), List.of(), List.of(), null, List.of(),
                filters, false);

        var result = new NearbyApartmentRankingSearchService(placeResolver, nearbySearch).search(analysis);

        assertThat(result.items()).extracting(item -> item.apartmentName()).containsExactly("twenty-eok");
        assertThat(result.items()).extracting(item -> item.metricValue()).containsExactly(205_000L);
    }

    private QuestionAnalysisResponse analysis(int limit) {
        return new QuestionAnalysisResponse("NEARBY_APARTMENT_RANKING", List.of(),
                new QuestionAnalysisResponse.AnalyzedPlace("홍대입구", "STATION"), "APARTMENT",
                "AVERAGE_PRICE", "DESC", limit, null, List.of(), List.of(), List.of());
    }

    private NearbyApartmentResponse.ApartmentCandidate candidate(String name, Long amount, int deals) {
        return new NearbyApartmentResponse.ApartmentCandidate(name, name, "마포구", "11440", "11440120",
                37.55, 126.92, 200, amount, null, deals, "2026-08-24", "2026-08-24");
    }
}
