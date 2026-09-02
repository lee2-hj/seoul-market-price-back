package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocation;
import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocationRepository;
import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FilteredRegionSummarySearchServiceTest {
    @Test
    void calculatesWeightedAverageAfterApplyingPyeongFilter() {
        ApartmentLocationRepository locations = mock(ApartmentLocationRepository.class);
        SggMasterRepository sggs = mock(SggMasterRepository.class);
        LocationMasterService locationService = mock(LocationMasterService.class);
        SggMaster gangdong = mock(SggMaster.class);
        when(gangdong.getSggCode()).thenReturn("11740");
        when(gangdong.getSggName()).thenReturn("강동구");
        when(sggs.findBySggName("강동구")).thenReturn(Optional.of(gangdong));
        when(locations.isAvailable()).thenReturn(true);
        when(locations.findByRegion("11740", null)).thenReturn(List.of(
                apartment("thirty-pyeong", 900_000L, 3, 100.0),
                apartment("twenty-pyeong", 500_000L, 2, 80.0)));

        var filters = new QuestionAnalysisResponse.SearchFilters(30.0, 39.0, null, null);
        var analysis = new QuestionAnalysisResponse("SINGLE_REGION", List.of(), null, "REGION",
                null, "AVERAGE_PRICE", null, 1, null, List.of(), List.of(), List.of(), null, List.of(),
                filters, false);

        var result = new FilteredRegionSummarySearchService(locations, sggs, locationService)
                .search("강동구에서 30평대 아파트 평균가격 알려줘", analysis);

        assertThat(result.summary()).contains("300,000만원");
        assertThat(result.keyPoints()).contains("조건: 30~39평", "거래 건수: 3건");
    }

    @Test
    void usesParquetPlaceNameWhenLocationCodesDoNotMatch() {
        ApartmentLocation expected = apartment("fallback", 600_000L, 2, 80.0);
        ApartmentLocationRepository locations = new ApartmentLocationRepository() {
            @Override public boolean isAvailable() { return true; }
            @Override public String datasetLocation() { return "test"; }
            @Override public List<ApartmentLocation> findCandidates(double latitude, double longitude, int radiusMeters) { return List.of(); }
            @Override public List<ApartmentLocation> findByRegion(String sggCode, String dongCode) { return List.of(); }
            @Override public List<ApartmentLocation> findByRegionName(String districtName, String dongName) { return List.of(expected); }
        };

        ApartmentLocationRepository.RegionLookup lookup = locations.findByRegionWithFallback(
                "different-code", "different-dong-code", "district", "dong");

        assertThat(lookup.strategy()).isEqualTo("PARQUET_NAME_FALLBACK");
        assertThat(lookup.locations()).containsExactly(expected);
    }

    private ApartmentLocation apartment(String name, long totalTradeAmount, int dealCount, double area) {
        return new ApartmentLocation(name, name, "강동구", "11740", "11740101", 37.5, 127.1,
                totalTradeAmount, dealCount, 10_000L, area, "2026-08-31", "2026-08-31");
    }
}
