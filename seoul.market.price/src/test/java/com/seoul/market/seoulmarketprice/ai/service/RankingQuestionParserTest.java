package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.RankingMetric;
import com.seoul.market.seoulmarketprice.ai.dto.SortDirection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankingQuestionParserTest {
    private final RankingQuestionParser parser = new RankingQuestionParser(
            Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"), ZoneId.of("Asia/Seoul")));

    @Test
    void parsesSharpDropRanking() {
        var query = parser.parse("최근 3개월 급락이 심했던 아파트 상위 5개 알려줘");

        assertEquals(RankingMetric.CHANGE_RATE, query.metric());
        assertEquals(SortDirection.ASC, query.direction());
        assertEquals(BigDecimal.valueOf(-10), query.threshold());
        assertEquals(5, query.limit());
        assertEquals("2026-05-24", query.from().toString());
        assertEquals("2026-08-24", query.to().toString());
    }

    @Test
    void parsesTradeVolumeRanking() {
        var query = parser.parse("최근 2주 거래량 많은 아파트 상위 10곳");

        assertEquals(RankingMetric.TRADE_COUNT, query.metric());
        assertEquals(SortDirection.DESC, query.direction());
        assertEquals(10, query.limit());
        assertEquals("2026-08-10", query.from().toString());
    }

    @Test
    void parsesPriceRange() {
        var query = parser.parse("최근 1개월 8억~12억 가격대 아파트 알려줘");

        assertEquals(RankingMetric.PRICE, query.metric());
        assertEquals(new BigDecimal("800000000"), query.minPrice());
        assertEquals(new BigDecimal("1200000000"), query.maxPrice());
    }

    @Test
    void parsesOneHundredEokMinimumPrice() {
        var query = parser.parse("강남구 100억 이상 아파트");

        assertEquals(RankingMetric.PRICE, query.metric());
        assertEquals(new BigDecimal("10000000000"), query.minPrice());
        assertEquals(null, query.maxPrice());
    }

    @Test
    void treatsExpensiveAsDescendingRatherThanMatchingIts싼Syllable() {
        var query = parser.parse("강동구에서 비싼 아파트 상위 5개 알려줘");

        assertEquals(RankingMetric.PRICE, query.metric());
        assertEquals(SortDirection.DESC, query.direction());
    }
}
