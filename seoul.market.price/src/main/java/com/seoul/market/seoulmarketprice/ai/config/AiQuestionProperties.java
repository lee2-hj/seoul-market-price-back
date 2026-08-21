package com.seoul.market.seoulmarketprice.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "app.ai.question")
public record AiQuestionProperties(List<String> allowedKeywords) {
    public AiQuestionProperties {
        allowedKeywords = allowedKeywords == null ? List.of() : List.copyOf(allowedKeywords);
    }
}
