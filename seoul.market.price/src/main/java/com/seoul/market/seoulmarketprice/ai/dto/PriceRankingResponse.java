package com.seoul.market.seoulmarketprice.ai.dto;

import java.util.List;

/** 평균 거래가 또는 평균 평당가 기준의 아파트 순위다. */
public record PriceRankingResponse(
        String regionName,
        String metricType,
        String baseDate,
        RankingCriteria criteria,
        List<Item> items
) {
    public record Item(
            int rank,
            String regionName,
            String apartmentName,
            Long metricValue,
            int dealCount
    ) {}
}
