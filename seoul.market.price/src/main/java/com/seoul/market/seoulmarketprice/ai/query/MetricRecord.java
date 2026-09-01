package com.seoul.market.seoulmarketprice.ai.query;

/** A source-independent, traceable row used by the generic query executor. */
public record MetricRecord(
        String sourceId,
        String districtName,
        String dongName,
        String apartmentName,
        Long averagePriceWon,
        Long averagePyeongPriceManwon,
        Double exclusiveAreaM2,
        Double pyeong,
        Long tradeCount,
        String latestDealDate,
        String baseDate
) {}
