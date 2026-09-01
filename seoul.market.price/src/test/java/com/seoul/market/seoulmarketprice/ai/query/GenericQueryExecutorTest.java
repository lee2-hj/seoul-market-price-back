package com.seoul.market.seoulmarketprice.ai.query;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenericQueryExecutorTest {
    private final GenericQueryExecutor executor = new GenericQueryExecutor();

    @Test
    void appliesAllFiltersBeforeSortingAndLimiting() {
        List<MetricRecord> result = executor.execute(List.of(
                        record("A", 500_000_000L, 25.0, 8L),
                        record("B", 650_000_000L, 24.0, 2L),
                        record("C", 700_000_000L, 34.0, 9L),
                        record("D", 600_000_000L, 26.0, 5L)),
                new QueryRequest(3L, 20.0, 29.9, 500_000_000L, 650_000_000L,
                        QueryRequest.SortField.AVERAGE_PRICE, false, 2));

        assertThat(result).extracting(MetricRecord::apartmentName).containsExactly("D", "A");
    }

    @Test
    void returnsEmptyWhenNoFactualRecordMatches() {
        List<MetricRecord> result = executor.execute(List.of(record("A", 500_000_000L, 25.0, 2L)),
                new QueryRequest(3L, null, null, null, null,
                        QueryRequest.SortField.TRADE_COUNT, false, 5));

        assertThat(result).isEmpty();
    }

    private MetricRecord record(String name, Long price, Double pyeong, Long count) {
        return new MetricRecord(name, "강남구", "대치동", name, price, 5_000L,
                84.0, pyeong, count, "2026-09-01", "2026-09-01");
    }
}
