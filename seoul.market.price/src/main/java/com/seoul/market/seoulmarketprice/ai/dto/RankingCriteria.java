package com.seoul.market.seoulmarketprice.ai.dto;

/** 순위 산출에 실제 적용된 기준을 화면에 그대로 전달한다. */
public record RankingCriteria(
        String metric,
        String unit,
        String period,
        int minimumTradeCount,
        String sortDirection
) {}
