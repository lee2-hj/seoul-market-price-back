package com.seoul.market.seoulmarketprice.ai.query;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RegionScopeResolver implements ScopeResolver {
    @Override
    public Optional<SearchScope> resolve(QuestionAnalysisResponse analysis) {
        if (analysis == null || analysis.regions() == null || analysis.regions().isEmpty()) return Optional.empty();
        List<QuestionAnalysisResponse.AnalyzedRegion> regions = analysis.regions();
        QuestionAnalysisResponse.AnalyzedRegion district = regions.stream()
                .filter(region -> "DISTRICT".equalsIgnoreCase(region.type()))
                .findFirst().orElse(null);
        QuestionAnalysisResponse.AnalyzedRegion dong = regions.stream()
                .filter(region -> "DONG".equalsIgnoreCase(region.type()))
                .findFirst().orElse(null);
        if (dong != null) {
            return Optional.of(new SearchScope(SearchScope.Type.DONG,
                    district == null ? null : district.name(), dong.name(), null, null));
        }
        if (district != null) {
            return Optional.of(new SearchScope(SearchScope.Type.DISTRICT, district.name(), null, null, null));
        }
        return Optional.empty();
    }
}
