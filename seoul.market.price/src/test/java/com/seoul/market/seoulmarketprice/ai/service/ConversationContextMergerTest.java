package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationContextMergerTest {
    private final ConversationContextMerger merger = new ConversationContextMerger();

    @Test
    void changesToExplicitDistrictWithoutKeepingPreviousPlace() {
        var previous = nearby("\uC218\uC720\uC5ED", null, null);
        var current = regional("\uAC15\uB0A8\uAD6C", "AVERAGE_PRICE", "DESC", null, null);

        var merged = merger.merge("\uAC15\uB0A8\uAD6C\uC5D0\uC11C \uC81C\uC77C \uBE44\uC2FC \uC544\uD30C\uD2B8", current, previous);

        assertThat(merged.analysis().regions()).extracting(QuestionAnalysisResponse.AnalyzedRegion::name)
                .containsExactly("\uAC15\uB0A8\uAD6C");
        assertThat(merged.analysis().referencePlace()).isNull();
        assertThat(merged.inheritedFromContext()).isEmpty();
    }

    @Test
    void keepsPreviousPlaceForExplicitThereReference() {
        var previous = nearby("\uC218\uC720\uC5ED", null, null);
        var current = regional(null, "AVERAGE_PRICE", "DESC", 20.0, 29.9);

        var merged = merger.merge("\uAC70\uAE30\uC11C 30\uD3C9\uB300\uB9CC", current, previous);

        assertThat(merged.analysis().referencePlace().name()).isEqualTo("\uC218\uC720\uC5ED");
        assertThat(merged.analysis().filters().minPyeong()).isEqualTo(20.0);
        assertThat(merged.analysis().filters().maxPyeong()).isEqualTo(29.9);
        assertThat(merged.inheritedFromContext()).contains("referencePlace");
    }

    @Test
    void keepsPreviousScopeForCriterionOnlyFollowUp() {
        var previous = regional("\uAC15\uB0A8\uAD6C", "AVERAGE_PRICE", null, null, null);
        var current = regional(null, "AVERAGE_PRICE", "DESC", null, null);

        var merged = merger.merge("\uAC00\uC7A5 \uBE44\uC2FC \uC21C\uC73C\uB85C", current, previous);

        assertThat(merged.analysis().regions()).extracting(QuestionAnalysisResponse.AnalyzedRegion::name)
                .containsExactly("\uAC15\uB0A8\uAD6C");
        assertThat(merged.inheritedFromContext()).contains("region");
    }

    @Test
    void currentFilterOverridesPreviousFilterInExplicitFollowUp() {
        var previous = regional("\uAC15\uB0A8\uAD6C", "AVERAGE_PRICE", "DESC", 20.0, 29.9);
        var current = regional(null, "AVERAGE_PRICE", "DESC", 30.0, 39.9);

        var merged = merger.merge("\uADF8 \uC9C0\uC5ED\uB3C4 30\uD3C9\uB300\uB85C", current, previous);

        assertThat(merged.analysis().filters().minPyeong()).isEqualTo(30.0);
        assertThat(merged.analysis().filters().maxPyeong()).isEqualTo(39.9);
        assertThat(merged.inheritedFromContext()).contains("region").doesNotContain("filters.minPyeong", "filters.maxPyeong");
    }

    @Test
    void independentQuestionDoesNotReuseAnyPreviousSlot() {
        var previous = nearby("\uC218\uC720\uC5ED", 1_000_000_000L, 1_100_000_000L);
        var current = regional("\uAC15\uB0A8\uAD6C", "AVERAGE_PRICE", "DESC", null, null);

        var merged = merger.merge("\uAC15\uB0A8\uAD6C \uC81C\uC77C \uBE44\uC2FC \uC544\uD30C\uD2B8", current, previous);

        assertThat(merged.analysis().referencePlace()).isNull();
        assertThat(merged.analysis().filters().minPriceWon()).isNull();
        assertThat(merged.inheritedFromContext()).isEmpty();
    }

    private QuestionAnalysisResponse nearby(String place, Long minPrice, Long maxPrice) {
        return new QuestionAnalysisResponse("NEARBY_APARTMENT_RANKING", List.of(),
                new QuestionAnalysisResponse.AnalyzedPlace(place, "STATION"), "APARTMENT", null,
                "AVERAGE_PRICE", "DESC", 5, null, List.of(), List.of(), List.of(), null, List.of(),
                new QuestionAnalysisResponse.SearchFilters(null, null, minPrice, maxPrice), false);
    }

    private QuestionAnalysisResponse regional(String region, String metric, String direction, Double minPyeong, Double maxPyeong) {
        List<QuestionAnalysisResponse.AnalyzedRegion> regions = region == null ? List.of()
                : List.of(new QuestionAnalysisResponse.AnalyzedRegion(region, "DISTRICT"));
        return new QuestionAnalysisResponse("APARTMENT_RANKING", regions, null, "APARTMENT", null,
                metric, direction, 5, null, List.of(), List.of(), region == null ? List.of("region") : List.of(),
                null, List.of(), new QuestionAnalysisResponse.SearchFilters(minPyeong, maxPyeong, null, null), region == null);
    }
}
