package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.RagAnswerRequest;
import com.seoul.market.seoulmarketprice.ai.dto.RagAnswerResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RagAnswerService {
    private final RestClient aiClient;

    public RagAnswerService(@Qualifier("aiFastApiRestClient") RestClient aiClient) {
        this.aiClient = aiClient;
    }

    public String answerIfSupported(String question) {
        RagAnswerResponse response = aiClient.post().uri("/ai/rag/answer")
                .body(new RagAnswerRequest(question)).retrieve().body(RagAnswerResponse.class);
        return response == null || response.answer() == null || response.answer().isBlank()
                ? null : response.answer().trim();
    }
}
