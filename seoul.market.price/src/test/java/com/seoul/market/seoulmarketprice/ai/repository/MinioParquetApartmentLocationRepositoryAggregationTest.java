package com.seoul.market.seoulmarketprice.ai.repository;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class MinioParquetApartmentLocationRepositoryAggregationTest {

    @Test
    void keepsOneDealAverageUnchanged() throws Exception {
        ApartmentLocation aggregated = aggregate(location(112_500L, 1));

        assertThat(aggregated.totalTradeAmount()).isEqualTo(112_500L);
        assertThat(aggregated.dealCount()).isOne();
        assertThat(aggregated.averageTradeAmount()).isEqualTo(112_500L);
    }

    @Test
    void calculatesAverageFromAccumulatedTotalForMultipleDeals() throws Exception {
        ApartmentLocation aggregated = aggregate(
                location(100_000L, 1), location(100_000L, 1), location(100_000L, 1),
                location(100_000L, 1), location(100_000L, 1));

        assertThat(aggregated.totalTradeAmount()).isEqualTo(500_000L);
        assertThat(aggregated.dealCount()).isEqualTo(5);
        assertThat(aggregated.averageTradeAmount()).isEqualTo(100_000L);
    }

    @Test
    void calculatesAverageFromOriginalTotalForTenDeals() throws Exception {
        ApartmentLocation aggregated = aggregate(location(1_600_000L, 10));

        assertThat(aggregated.totalTradeAmount()).isEqualTo(1_600_000L);
        assertThat(aggregated.dealCount()).isEqualTo(10);
        assertThat(aggregated.averageTradeAmount()).isEqualTo(160_000L);
    }

    private ApartmentLocation aggregate(ApartmentLocation... locations) throws Exception {
        Class<?> accumulatorType = Arrays.stream(MinioParquetApartmentLocationRepository.class.getDeclaredClasses())
                .filter(type -> type.getSimpleName().equals("ApartmentAccumulator"))
                .findFirst()
                .orElseThrow();
        Constructor<?> constructor = accumulatorType.getDeclaredConstructor(ApartmentLocation.class);
        Method add = accumulatorType.getDeclaredMethod("add", ApartmentLocation.class);
        Method toLocation = accumulatorType.getDeclaredMethod("toLocation");
        constructor.setAccessible(true);
        add.setAccessible(true);
        toLocation.setAccessible(true);

        Object accumulator = constructor.newInstance(locations[0]);
        for (ApartmentLocation location : locations) {
            add.invoke(accumulator, location);
        }
        return (ApartmentLocation) toLocation.invoke(accumulator);
    }

    private ApartmentLocation location(long totalTradeAmount, int dealCount) {
        return new ApartmentLocation("apt-1", "Test Apartment", "Test address", "11110", "11110101",
                "District", "Dong", 37.5, 127.0, totalTradeAmount, dealCount,
                5_000L, 84.0, "2026-09-01", "2026-09-01");
    }
}
