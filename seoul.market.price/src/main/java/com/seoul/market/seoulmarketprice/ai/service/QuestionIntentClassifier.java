package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.config.AiQuestionProperties;
import org.springframework.stereotype.Component;

@Component
public class QuestionIntentClassifier {
    public enum Intent { PRICE_COMPARISON, SINGLE_REGION, TOP_BOTTOM }
    private final AiQuestionProperties properties;

    public QuestionIntentClassifier(AiQuestionProperties properties) {
        this.properties = properties;
    }

    public void validateScope(String question) {
        if (question == null || properties.allowedKeywords().stream().noneMatch(question::contains)) {
            throw new IllegalArgumentException("서울시 아파트 가격 외 질문은 답변할 수 없습니다.");
        }
    }

    public Intent classify(String question) {
        validateScope(question);
        boolean rankingExpression = question.contains("최고") || question.contains("최저")
                || ((question.contains("가장") || question.contains("제일"))
                && (question.contains("높") || question.contains("낮") || question.contains("비싸")
                || question.contains("저렴") || question.contains("싼")));
        if (rankingExpression && question.contains("구")) {
            return Intent.TOP_BOTTOM;
        }
        long regionCount = java.util.regex.Pattern.compile("[가-힣]+구\\s+[가-힣]+동")
                .matcher(question).results().count();
        if (regionCount >= 2) return Intent.PRICE_COMPARISON;
        if (regionCount == 1) return Intent.SINGLE_REGION;
        throw new IllegalArgumentException("지역을 찾지 못했습니다. 자치구와 동을 함께 입력해주세요.");
    }
}
