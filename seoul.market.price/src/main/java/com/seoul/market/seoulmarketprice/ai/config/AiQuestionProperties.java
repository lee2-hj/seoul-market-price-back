package com.seoul.market.seoulmarketprice.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "app.ai.question")
/** AI 질문 분석에서 허용할 키워드 설정을 보유한다. */
public record AiQuestionProperties(List<String> allowedKeywords) {
    public AiQuestionProperties {
        allowedKeywords = allowedKeywords == null ? List.of() : List.copyOf(allowedKeywords);
    }
}
