package com.seoul.market.seoulmarketprice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${fastapi.url}")
    private String fastApiUrl;

    @Bean
    public RestClient fastApiRestClient() {
        return RestClient.builder()
                .baseUrl(fastApiUrl)
                .build();
    }

    @Bean
    public RestClient aiFastApiRestClient(
            @Value("${fastapi.ai-url:http://localhost:8001}") String aiFastApiUrl
    ) {
        return RestClient.builder().baseUrl(aiFastApiUrl).build();
    }
}
