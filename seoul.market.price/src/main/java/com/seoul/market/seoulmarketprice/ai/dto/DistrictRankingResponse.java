package com.seoul.market.seoulmarketprice.ai.dto;

import java.util.List;

/** 서울 자치구를 거래 건수 가중 평균 평단가로 정렬한 결과다. */
public record DistrictRankingResponse(
        String regionName,
        String metricType,
        String baseDate,
        RankingCriteria criteria,
        List<Item> items
) {
    public record Item(int rank, String districtName, Long averagePyeongAmount, long dealCount) {}
}
