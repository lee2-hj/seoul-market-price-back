package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisRequest;
import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class QuestionAnalysisService {
    private final RestClient aiClient;

    public QuestionAnalysisService(@Qualifier("aiFastApiRestClient") RestClient aiClient) {
        this.aiClient = aiClient;
    }

    public QuestionAnalysisResponse analyze(String question) {
        QuestionAnalysisResponse response = aiClient.post()
                .uri("/ai/analyze-question")
                .body(new QuestionAnalysisRequest(question))
                .retrieve()
                .body(QuestionAnalysisResponse.class);
        if (response == null) {
            throw new IllegalStateException("질문 분석 서버가 빈 응답을 반환했습니다.");
        }
        return response;
    }
}
