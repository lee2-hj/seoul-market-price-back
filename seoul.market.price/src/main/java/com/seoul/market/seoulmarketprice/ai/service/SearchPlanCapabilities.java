package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.AiExecutionPlan;

import java.util.Set;

/** Declares which structured conditions each search tool can execute without silently ignoring them. */
final class SearchPlanCapabilities {
    private static final Set<String> FILTER_CAPABLE = Set.of(
            "SINGLE_REGION", "APARTMENT_RANKING", "NEARBY_APARTMENT_RANKING");

    private SearchPlanCapabilities() {}

    static void validate(AiExecutionPlan plan) {
        if (!hasFilters(plan.filters()) || FILTER_CAPABLE.contains(plan.intent())) return;
        throw new IllegalArgumentException(plan.intent() + " 검색은 현재 가격·면적 조건을 함께 처리할 수 없습니다.");
    }

    private static boolean hasFilters(AiExecutionPlan.Filters filters) {
        return filters.minPyeong() != null || filters.maxPyeong() != null
                || filters.minPriceWon() != null || filters.maxPriceWon() != null;
    }
}
