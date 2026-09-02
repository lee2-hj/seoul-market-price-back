package com.seoul.market.seoulmarketprice.ai.query;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PlaceScopeResolver implements ScopeResolver {
    @Override
    public Optional<SearchScope> resolve(QuestionAnalysisResponse analysis) {
        if (analysis == null || analysis.referencePlace() == null
                || analysis.referencePlace().name() == null || analysis.referencePlace().name().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new SearchScope(SearchScope.Type.PLACE_RADIUS, null, null,
                analysis.referencePlace().name(), null));
    }
}
