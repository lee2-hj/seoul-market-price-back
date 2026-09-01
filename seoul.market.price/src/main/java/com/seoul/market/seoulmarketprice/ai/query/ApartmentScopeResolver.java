package com.seoul.market.seoulmarketprice.ai.query;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ApartmentScopeResolver implements ScopeResolver {
    @Override
    public Optional<SearchScope> resolve(QuestionAnalysisResponse analysis) {
        if (analysis == null || analysis.apartmentName() == null || analysis.apartmentName().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new SearchScope(SearchScope.Type.APARTMENT, null, null, null, analysis.apartmentName()));
    }
}
