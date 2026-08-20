package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.PriceComparisonRequest;
import com.seoul.market.seoulmarketprice.ai.dto.PriceComparisonResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Qualifier;

@Service
public class AiExplanationService {
    private final RestClient aiFastApiRestClient;

    public AiExplanationService(@Qualifier("aiFastApiRestClient") RestClient aiFastApiRestClient) {
        this.aiFastApiRestClient = aiFastApiRestClient;
    }

    public PriceComparisonResponse explain(PriceComparisonRequest request) {
        return aiFastApiRestClient.post()
                .uri("/ai/explain-price-comparison")
                .body(request)
                .retrieve()
                .body(PriceComparisonResponse.class);
    }
}
