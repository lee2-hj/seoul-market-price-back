package com.seoul.market.seoulmarketprice.fastapi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RegionAptCompareResponse(
        AptGroupDto aptGroup1,
        AptGroupDto aptGroup2
) {
    public record AptGroupDto(
            @JsonProperty("apt_name")
            String aptName,

            @JsonProperty("avg_deal_price")
            Long avgDealPrice,

            @JsonProperty("avg_pyeong_price")
            Long avgPyeongPrice,

            @JsonProperty("avg_pyeong")
            Integer avgPyeong,

            @JsonProperty("latest_trade_pyeong")
            Integer latestTradePyeong,

            @JsonProperty("deal_count")
            Integer dealCount,

            @JsonProperty("total_households")
            Integer totalHouseholds,

            @JsonProperty("build_year")
            Integer buildYear,

            @JsonProperty("use_approval_date")
            String useApprovalDate
    ){}
}
