package com.seoul.market.seoulmarketprice.ai.query;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScopeResolverChainTest {
    private final ScopeResolverChain chain = new ScopeResolverChain(List.of(
            new RegionScopeResolver(), new PlaceScopeResolver(), new ApartmentScopeResolver()));

    @Test
    void apartmentScopeHasPriorityOverRegionScope() {
        QuestionAnalysisResponse analysis = new QuestionAnalysisResponse("APARTMENT_DETAIL", List.of(
                new QuestionAnalysisResponse.AnalyzedRegion("강남구", "DISTRICT")), null,
                "APARTMENT", "신현대12차", "AVERAGE_PRICE", null, 1, null,
                List.of(), List.of(), List.of(), null, List.of(),
                new QuestionAnalysisResponse.SearchFilters(null, null, null, null), false);

        SearchScope scope = chain.resolve(analysis);

        assertThat(scope.type()).isEqualTo(SearchScope.Type.APARTMENT);
        assertThat(scope.apartmentName()).isEqualTo("신현대12차");
    }

    @Test
    void unresolvedAnalysisDoesNotPretendToBeAllSeoul() {
        SearchScope scope = chain.resolve(null);

        assertThat(scope.type()).isEqualTo(SearchScope.Type.UNRESOLVED);
    }
}
