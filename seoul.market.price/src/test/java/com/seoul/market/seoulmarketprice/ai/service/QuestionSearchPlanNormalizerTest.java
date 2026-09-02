package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionSearchPlanNormalizerTest {
    private final QuestionSearchPlanNormalizer normalizer = new QuestionSearchPlanNormalizer();

    @Test
    void turnsLowPriceApartmentWordingIntoAscendingRanking() {
        var plan = normalizer.normalize("중구에서 가장 싼 아파트 찾아줘", analysis("APARTMENT_RANKING", "DESC"));

        assertThat(plan.intent()).isEqualTo("APARTMENT_RANKING");
        assertThat(plan.direction()).isEqualTo("ASC");
        assertThat(plan.toolPlan()).containsExactly("RESOLVE_REGION", "GET_APARTMENT_RANKING");
    }

    @Test
    void turnsPyeongBandAverageQuestionIntoFilteredRegionalSummary() {
        var plan = normalizer.normalize("강동구에서 30평대 아파트 평균가격 알려줘", analysis("UNSUPPORTED", null));

        assertThat(plan.intent()).isEqualTo("SINGLE_REGION");
        assertThat(plan.target()).isEqualTo("REGION");
        assertThat(plan.filters().minPyeong()).isEqualTo(30.0);
        assertThat(plan.filters().maxPyeong()).isEqualTo(39.0);
        assertThat(plan.toolPlan()).containsExactly("RESOLVE_REGION", "GET_FILTERED_REGION_SUMMARY");
    }

    @Test
    void turnsPriceBandApartmentQuestionIntoSearchFilter() {
        var plan = normalizer.normalize("강북구 5억대 아파트 알려줘", analysis("UNSUPPORTED", null));

        assertThat(plan.intent()).isEqualTo("APARTMENT_RANKING");
        assertThat(plan.filters().minPriceWon()).isEqualTo(500_000_000L);
        assertThat(plan.filters().maxPriceWon()).isEqualTo(600_000_000L);
    }

    @Test
    void createsPyeongApartmentPlanWithoutAnLlmAnalysis() {
        var plan = normalizer.fromExplicitQuestion("대치동 20평대 아파트 알려줘");

        assertThat(plan).isNotNull();
        assertThat(plan.intent()).isEqualTo("APARTMENT_RANKING");
        assertThat(plan.filters().minPyeong()).isEqualTo(20.0);
        assertThat(plan.filters().maxPyeong()).isEqualTo(29.0);
    }

    private QuestionAnalysisResponse analysis(String intent, String direction) {
        return new QuestionAnalysisResponse(intent, List.of(), null, null, null, "AVERAGE_PRICE", direction,
                5, null, List.of(), List.of(), List.of("question"), null, List.of(),
                new QuestionAnalysisResponse.SearchFilters(null, null, null, null), true);
    }
}
