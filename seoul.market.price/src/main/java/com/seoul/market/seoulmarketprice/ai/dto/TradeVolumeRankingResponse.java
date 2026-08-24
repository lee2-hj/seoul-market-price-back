package com.seoul.market.seoulmarketprice.ai.dto;

import java.util.List;

/** 특정 자치구 또는 자치동의 거래량 상위 아파트 목록이다. */
public record TradeVolumeRankingResponse(
        String regionName,
        String periodStart,
        String periodEnd,
        int totalDealCount,
        RankingCriteria criteria,
        List<Item> items
) {
    public record Item(
            int rank,
            String regionName,
            String apartmentName,
            String mainAddressNumber,
            String subAddressNumber,
            int dealCount,
            Long averageTradeAmount
    ) {}
}
