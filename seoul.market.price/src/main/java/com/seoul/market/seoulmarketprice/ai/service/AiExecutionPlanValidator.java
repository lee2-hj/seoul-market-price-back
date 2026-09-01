package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.AiExecutionPlan;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AiExecutionPlanValidator {
    private static final Set<String> SUPPORTED = Set.of("PRICE_COMPARISON", "SINGLE_REGION", "DISTRICT_RANKING",
            "APARTMENT_RANKING", "APARTMENT_DETAIL", "NEARBY_APARTMENT_RANKING", "NEAREST_APARTMENT_PRICE", "TRADE_VOLUME", "TRADE_TREND");

    public void validate(AiExecutionPlan plan) {
        if (!SUPPORTED.contains(plan.intent())) return;
        AiExecutionPlan.Filters filters = plan.filters();
        if (filters.minPyeong() != null && filters.maxPyeong() != null && filters.minPyeong() > filters.maxPyeong())
            throw new IllegalArgumentException("면적 최소값이 최대값보다 클 수 없습니다.");
        if (filters.minPriceWon() != null && filters.maxPriceWon() != null && filters.minPriceWon() > filters.maxPriceWon())
            throw new IllegalArgumentException("가격 최소값이 최대값보다 클 수 없습니다.");
        if (plan.limit() < 1 || plan.limit() > 100) throw new IllegalArgumentException("조회 개수는 1~100개여야 합니다.");
        SearchPlanCapabilities.validate(plan);
    }
}
