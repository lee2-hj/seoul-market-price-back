package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.RankingMetric;
import com.seoul.market.seoulmarketprice.ai.dto.RankingSearchQuery;
import com.seoul.market.seoulmarketprice.ai.dto.RankingTarget;
import com.seoul.market.seoulmarketprice.ai.dto.SortDirection;
import com.seoul.market.seoulmarketprice.ai.query.GenericQueryExecutor;
import com.seoul.market.seoulmarketprice.ai.query.MetricRecord;
import com.seoul.market.seoulmarketprice.ai.query.ParquetApartmentMetricDataSourceAdapter;
import com.seoul.market.seoulmarketprice.fastapi.service.FastApiService;
import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PriceRankingSearchServiceParquetTest {

    @Test
    void ranksByAveragePriceRatherThanTradeCount() {
        RankingQuestionParser parser = mock(RankingQuestionParser.class);
        SggMasterRepository sggRepository = mock(SggMasterRepository.class);
        LocationMasterService locationService = mock(LocationMasterService.class);
        FastApiService fastApiService = mock(FastApiService.class);
        ParquetApartmentMetricDataSourceAdapter parquetAdapter = mock(ParquetApartmentMetricDataSourceAdapter.class);
        SggMaster gangnamGu = mock(SggMaster.class);
        when(gangnamGu.getSggCode()).thenReturn("11680");
        when(gangnamGu.getSggName()).thenReturn("강남구");
        when(sggRepository.findBySggName("강남구")).thenReturn(Optional.of(gangnamGu));
        when(parquetAdapter.isAvailable()).thenReturn(true);
        when(parquetAdapter.fetchByRegion("11680", null, "강남구", null)).thenReturn(List.of(
                new MetricRecord("high", "강남구", "압구정동", "신현대12차", "강남구 압구정동",
                        11_250_000_000L, 0L, 180.0, 54.4, 6L, "2026-08-31", "2026-09-01"),
                new MetricRecord("low", "강남구", "역삼동", "대명벨리온", "강남구 역삼동",
                        160_000_000L, 0L, 40.0, 12.1, 10L, "2026-08-31", "2026-09-01")
        ));
        RankingSearchQuery query = new RankingSearchQuery(RankingTarget.APARTMENT, RankingMetric.PRICE,
                SortDirection.DESC, null, LocalDate.now().minusMonths(1), LocalDate.now(),
                null, null, null, null, null, 2, 0);

        var result = new PriceRankingSearchService(parser, sggRepository, locationService, fastApiService,
                parquetAdapter, new GenericQueryExecutor()).search("강남구에서 가장 비싼 아파트", query);

        assertThat(result.items()).extracting(item -> item.apartmentName())
                .containsExactly("신현대12차", "대명벨리온");
        assertThat(result.items()).extracting(item -> item.metricValue())
                .containsExactly(1_125_000L, 16_000L);
        verifyNoInteractions(fastApiService);
    }

    @Test
    void usesParquetRowsToIncludePriceAndPyeongForUnfilteredPriceRanking() {
        RankingQuestionParser parser = mock(RankingQuestionParser.class);
        SggMasterRepository sggRepository = mock(SggMasterRepository.class);
        LocationMasterService locationService = mock(LocationMasterService.class);
        FastApiService fastApiService = mock(FastApiService.class);
        ParquetApartmentMetricDataSourceAdapter parquetAdapter = mock(ParquetApartmentMetricDataSourceAdapter.class);
        SggMaster jungGu = mock(SggMaster.class);
        when(jungGu.getSggCode()).thenReturn("11140");
        when(jungGu.getSggName()).thenReturn("중구");
        when(sggRepository.findBySggName("중구")).thenReturn(Optional.of(jungGu));
        when(parquetAdapter.isAvailable()).thenReturn(true);
        when(parquetAdapter.fetchByRegion("11140", null, "중구", null)).thenReturn(List.of(
                new MetricRecord("row-1", "중구", "황학동", "한양I-Class", "중구 황학동",
                        125_000_000L, 4_860L, 84.93, 25.69, 2L, "2026-08-20", "2026-09-01")
        ));
        RankingSearchQuery query = new RankingSearchQuery(RankingTarget.APARTMENT, RankingMetric.PRICE,
                SortDirection.ASC, null, LocalDate.now().minusMonths(1), LocalDate.now(),
                null, null, null, null, null, 1, 0);

        var result = new PriceRankingSearchService(parser, sggRepository, locationService, fastApiService,
                parquetAdapter, new GenericQueryExecutor()).search("중구에서 가장 싼 아파트 찾아줘", query);

        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.metricValue()).isEqualTo(12_500L);
            assertThat(item.exclusiveAreaM2()).isEqualTo(84.93);
            assertThat(item.pyeong()).isEqualTo(25.69);
        });
        verifyNoInteractions(fastApiService);
    }
}
