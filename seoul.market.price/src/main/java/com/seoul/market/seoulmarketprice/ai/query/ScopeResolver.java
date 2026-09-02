package com.seoul.market.seoulmarketprice.ai.query;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;

import java.util.Optional;

/** One resolver owns one way of turning an analysed question into a data boundary. */
public interface ScopeResolver {
    Optional<SearchScope> resolve(QuestionAnalysisResponse analysis);
}
