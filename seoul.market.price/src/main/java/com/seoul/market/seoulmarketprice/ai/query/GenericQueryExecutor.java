package com.seoul.market.seoulmarketprice.ai.query;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Applies filtering, sorting, and limiting consistently after data retrieval. */
@Component
public class GenericQueryExecutor {
    public List<MetricRecord> execute(List<MetricRecord> source, QueryRequest request) {
        Comparator<MetricRecord> comparator = Comparator.comparing(sortValue(request.sortField()),
                Comparator.nullsLast(Comparator.naturalOrder()));
        if (!request.ascending()) comparator = comparator.reversed();

        return source.stream()
                .filter(Objects::nonNull)
                .filter(record -> request.minimumTradeCount() == null
                        || value(record.tradeCount()) >= request.minimumTradeCount())
                .filter(record -> inRange(record.pyeong(), request.minPyeong(), request.maxPyeong()))
                .filter(record -> inRange(record.averagePriceWon(), request.minAveragePriceWon(), request.maxAveragePriceWon()))
                .sorted(comparator.thenComparing(record -> safe(record.sourceId())))
                .limit(request.limit())
                .toList();
    }

    private Function<MetricRecord, Long> sortValue(QueryRequest.SortField field) {
        return switch (field) {
            case AVERAGE_PRICE -> MetricRecord::averagePriceWon;
            case AVERAGE_PYEONG_PRICE -> MetricRecord::averagePyeongPriceManwon;
            case TRADE_COUNT -> MetricRecord::tradeCount;
            case PYEONG -> record -> record.pyeong() == null ? null : Math.round(record.pyeong() * 100);
        };
    }

    private boolean inRange(Double value, Double min, Double max) {
        if (min == null && max == null) return true;
        if (value == null) return false;
        return (min == null || value >= min) && (max == null || value <= max);
    }

    private boolean inRange(Long value, Long min, Long max) {
        if (min == null && max == null) return true;
        if (value == null) return false;
        return (min == null || value >= min) && (max == null || value <= max);
    }

    private long value(Long value) {
        return value == null ? Long.MIN_VALUE : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
