package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.AiExecutionPlan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiExecutionPlanValidatorTest {
    private final AiExecutionPlanValidator validator = new AiExecutionPlanValidator();

    @Test
    void allowsFiltersForConditionalRegionSummary() {
        assertThatCode(() -> validator.validate(plan("SINGLE_REGION", 30.0, 39.0)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsFiltersForAPlanThatCannotApplyThem() {
        assertThatThrownBy(() -> validator.validate(plan("TRADE_TREND", 30.0, 39.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("가격·면적 조건");
    }

    private AiExecutionPlan plan(String intent, Double minPyeong, Double maxPyeong) {
        return new AiExecutionPlan(intent, new AiExecutionPlan.Scope(null, null, null, null),
                new AiExecutionPlan.Filters(minPyeong, maxPyeong, null, null, 0, null, null),
                new AiExecutionPlan.Sort("AVERAGE_PRICE", "DESC"), 5, List.of(), false, List.of());
    }
}
