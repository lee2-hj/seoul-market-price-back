package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;

import java.util.Optional;

/** Short-lived conversation state. A Redis implementation can replace this interface later. */
public interface ConversationContextStore {
    Optional<QuestionAnalysisResponse> get(String sessionId);
    void put(String sessionId, QuestionAnalysisResponse analysis);
    void evict(String sessionId);
}
