package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.config.AiQuestionProperties;
import org.springframework.stereotype.Component;

@Component
public class QuestionIntentClassifier {
    public enum Intent { PRICE_COMPARISON, SINGLE_REGION, DISTRICT_SUMMARY, CITY_SUMMARY, DISTRICT_RANKING, TOP_BOTTOM, RANKING_SEARCH, TRADE_TREND }
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
        if (question.contains("자치구") && (question.contains("평단가") || question.contains("평당가"))) {
            return Intent.DISTRICT_RANKING;
        }
        if (isCitySummaryRequest(question)) {
            return Intent.CITY_SUMMARY;
        }
        if (question.contains("거래 동향") || question.contains("거래 추이") || question.contains("거래가 늘") || question.contains("거래가 줄")) {
            return Intent.TRADE_TREND;
        }
        if (question.contains("거래량")) {
            return Intent.RANKING_SEARCH;
        }
        if (question.contains("아파트") && (question.contains("비싼") || question.contains("비싸")
                || question.contains("고가") || question.contains("저렴") || question.contains("싼"))) {
            return Intent.RANKING_SEARCH;
        }
        boolean rankingExpression = question.contains("최고") || question.contains("최저")
                || ((question.contains("가장") || question.contains("제일"))
                && (question.contains("높") || question.contains("낮") || question.contains("비싸")
                || question.contains("저렴") || question.contains("싼")));
        if (rankingExpression && question.contains("구")) {
            return Intent.TOP_BOTTOM;
        }
        long regionCount = RegionQuestionPatterns.FULL_REGION.matcher(question).results().count();
        if (regionCount >= 2) return Intent.PRICE_COMPARISON;
        if (regionCount == 1) return Intent.SINGLE_REGION;
        long districtCount = RegionQuestionPatterns.DISTRICT.matcher(question).results().count();
        if (districtCount >= 2) return Intent.PRICE_COMPARISON;
        if (districtCount == 1) {
            return Intent.DISTRICT_SUMMARY;
        }
        throw new IllegalArgumentException("지역을 찾지 못했습니다. 자치구와 동을 함께 입력해주세요.");
    }

    private boolean isCitySummaryRequest(String question) {
        boolean mentionsSeoul = question.contains("서울");
        boolean requestsAverage = question.contains("평균") || question.contains("평균가");
        boolean hasDistrict = RegionQuestionPatterns.DISTRICT.matcher(question).find();
        return mentionsSeoul && requestsAverage && !hasDistrict;
    }
}
