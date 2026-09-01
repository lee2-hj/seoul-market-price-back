package com.seoul.market.seoulmarketprice.ai.query;

/** Conditions must be applied in code, after the data source has returned factual rows. */
public record QueryRequest(
        Long minimumTradeCount,
        Double minPyeong,
        Double maxPyeong,
        Long minAveragePriceWon,
        Long maxAveragePriceWon,
        SortField sortField,
        boolean ascending,
        int limit
) {
    public enum SortField { AVERAGE_PRICE, AVERAGE_PYEONG_PRICE, TRADE_COUNT, PYEONG }
}
