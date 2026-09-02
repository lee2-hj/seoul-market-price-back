package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.RagAnswerRequest;
import com.seoul.market.seoulmarketprice.ai.dto.RagAnswerResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class RagAnswerService {
    private static final List<String> SUPPORTED_DOMAIN_MARKERS = List.of(
            "아파트", "부동산", "가격", "시세", "거래가", "평단가", "평당", "평균",
            "단지", "지역", "지도", "장소", "검색", "서비스", "데이터", "기준"
    );
    private final RestClient aiClient;

    public RagAnswerService(@Qualifier("aiFastApiRestClient") RestClient aiClient) {
        this.aiClient = aiClient;
    }

    public String answerIfSupported(String question) {
        if (!supportsQuestion(question)) return null;
        RagAnswerResponse response = aiClient.post().uri("/ai/rag/answer")
                .body(new RagAnswerRequest(question)).retrieve().body(RagAnswerResponse.class);
        return response == null || response.answer() == null || response.answer().isBlank()
                ? null : response.answer().trim();
    }

    static boolean supportsQuestion(String question) {
        if (question == null || question.isBlank()) return false;
        return SUPPORTED_DOMAIN_MARKERS.stream().anyMatch(question::contains);
    }
}
