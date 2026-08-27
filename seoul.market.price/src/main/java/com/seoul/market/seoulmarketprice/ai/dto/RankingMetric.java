package com.seoul.market.seoulmarketprice.ai.dto;

/** 순위를 결정하는 값이다. CHANGE_RATE는 상승률과 하락률을 함께 표현한다. */
public enum RankingMetric {
    PRICE,
    CHANGE_RATE,
    TRADE_COUNT,
    POPULARITY
}
