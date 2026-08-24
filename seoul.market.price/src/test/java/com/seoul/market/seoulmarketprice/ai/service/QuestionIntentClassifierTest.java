package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.config.AiQuestionProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.seoul.market.seoulmarketprice.ai.service.QuestionIntentClassifier.Intent.*;
import static org.junit.jupiter.api.Assertions.*;

class QuestionIntentClassifierTest {
    private final QuestionIntentClassifier classifier = new QuestionIntentClassifier(
            new AiQuestionProperties(List.of("아파트", "가격", "시세", "매매가", "평당가", "평단가", "거래량",
                    "거래가", "전세가", "비교", "최고", "최저", "가장 높은", "가장 낮은")));

    @Test
    void classifiesComparisonQuestions() {
        assertEquals(PRICE_COMPARISON, classifier.classify("마포구 서교동과 성동구 성수동 가격 비교해줘"));
        assertEquals(PRICE_COMPARISON, classifier.classify("강동구 강서구 비교"));
        assertEquals(PRICE_COMPARISON, classifier.classify("강동구 성동구 비교"));
    }

    @Test
    void classifiesSingleRegionQuestions() {
        assertEquals(SINGLE_REGION, classifier.classify("마포구 서교동 평균 가격 알려줘"));
    }

    @Test
    void doesNotTreatGenericJachidongAsAnActualDongName() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> classifier.classify("자치동 가격 알려줘"));
        assertEquals("지역을 찾지 못했습니다. 자치구와 동을 함께 입력해주세요.", exception.getMessage());
        assertEquals(SINGLE_REGION, classifier.classify("성동구 성수동 가격 알려줘"));
    }

    @Test
    void classifiesDistrictSummaryWithoutTreatingGangdongAsDong() {
        assertEquals(DISTRICT_SUMMARY, classifier.classify("강동구 아파트 평균가 알려줘"));
    }

    @Test
    void classifiesFlexibleTopBottomExpressions() {
        assertEquals(TOP_BOTTOM, classifier.classify("노원구에서 가장 평단가가 높은 동은 어디야?"));
        assertEquals(TOP_BOTTOM, classifier.classify("강남구에서 평단가가 가장 낮은 동 알려줘"));
        assertEquals(TOP_BOTTOM, classifier.classify("송파구 최고 평단가 동은?"));
        assertEquals(TOP_BOTTOM, classifier.classify("도봉구 최저 평단가 동은?"));
    }

    @Test
    void classifiesSeoulDistrictPyeongRankingSeparately() {
        assertEquals(DISTRICT_RANKING, classifier.classify("평단가 높은 자치구 5개 알려줘"));
        assertEquals(DISTRICT_RANKING, classifier.classify("평당가 낮은 자치구 3곳 알려줘"));
    }

    @Test
    void classifiesTradeVolumeRankingQuestions() {
        assertEquals(RANKING_SEARCH, classifier.classify("강남구에서 거래량이 많은 아파트 상위 2개 알려줘"));
        assertEquals(RANKING_SEARCH, classifier.classify("거래량 상위 5개 아파트 알려줘"));
    }

    @Test
    void rejectsUnsupportedQuestions() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> classifier.classify("오늘 서울 날씨 알려줘"));
        assertEquals("서울시 아파트 가격 외 질문은 답변할 수 없습니다.", exception.getMessage());
    }

    @Test
    void explainsMissingRegion() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> classifier.classify("서울 아파트 가격 알려줘"));
        assertEquals("지역을 찾지 못했습니다. 자치구와 동을 함께 입력해주세요.", exception.getMessage());
    }
}
