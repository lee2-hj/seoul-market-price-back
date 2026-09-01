package com.seoul.market.seoulmarketprice.ai.query;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/** Ordered chain: explicit apartment/place scopes win before broad regional scopes. */
@Component
public class ScopeResolverChain {
    private final List<ScopeResolver> resolvers;

    public ScopeResolverChain(List<ScopeResolver> resolvers) {
        this.resolvers = resolvers.stream()
                .sorted(Comparator.comparingInt(this::priority))
                .toList();
    }

    public SearchScope resolve(QuestionAnalysisResponse analysis) {
        return resolvers.stream()
                .map(resolver -> resolver.resolve(analysis))
                .flatMap(java.util.Optional::stream)
                .findFirst()
                .orElse(new SearchScope(SearchScope.Type.UNRESOLVED, null, null, null, null));
    }

    private int priority(ScopeResolver resolver) {
        if (resolver instanceof ApartmentScopeResolver) return 0;
        if (resolver instanceof PlaceScopeResolver) return 1;
        if (resolver instanceof RegionScopeResolver) return 2;
        return 100;
    }
}
