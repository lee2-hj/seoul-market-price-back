package com.seoul.market.seoulmarketprice.ai.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagAnswerServiceTest {

    @Test
    void allowsOnlyApartmentOrServiceGuideQuestions() {
        assertTrue(RagAnswerService.supportsQuestion("평균 거래가는 어떤 기준으로 계산하나요?"));
        assertTrue(RagAnswerService.supportsQuestion("아파트 가격 검색은 어떻게 하나요?"));
        assertFalse(RagAnswerService.supportsQuestion("너의 이름은?"));
        assertFalse(RagAnswerService.supportsQuestion("오늘 날씨 알려줘"));
    }
}
