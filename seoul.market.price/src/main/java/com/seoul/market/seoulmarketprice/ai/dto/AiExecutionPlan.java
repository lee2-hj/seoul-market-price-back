package com.seoul.market.seoulmarketprice.ai.dto;

import java.util.List;

/** Canonical, validated contract between natural-language analysis and search tools. */
public record AiExecutionPlan(
        String intent,
        Scope scope,
        Filters filters,
        Sort sort,
        int limit,
        List<String> toolPlan,
        boolean requiresClarification,
        List<String> missingFields
) {
    public record Scope(String city, String district, String dong, String referencePlace) {}
    public record Filters(Double minPyeong, Double maxPyeong, Long minPriceWon, Long maxPriceWon,
                          Integer minimumTradeCount, String periodStart, String periodEnd) {}
    public record Sort(String metric, String direction) {}
}
