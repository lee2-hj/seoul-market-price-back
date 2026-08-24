package com.seoul.market.seoulmarketprice.fastapi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AptMktResponse(
        @JsonProperty("status")
        String status,

        @JsonProperty("search_period")
        SearchPeriodDto searchPeriod,

        @JsonProperty("count")
        Integer count,

        @JsonProperty("data")
        List<AptMktDataDto> data
) {
    public record SearchPeriodDto(
            @JsonProperty("start_date")
            String startDate,

            @JsonProperty("end_date")
            String endDate
    ){}

    public record AptMktDataDto(
            @JsonProperty("apt_name")
            String aptName,

            @JsonProperty("cgg_cd")
            String cggCd,

            @JsonProperty("cgg_nm")
            String cggNm,

            @JsonProperty("stdg_cd")
            String stdgCd,

            @JsonProperty("stdg_nm")
            String stdgNm,

            @JsonProperty("total_deal_count")
            Integer totalDealCount,

            @JsonProperty("total_deal_amount")
            Long totalDealAmount,

            @JsonProperty("average_deal_price")
            Long averageDealPrice,

            @JsonProperty("max_deal_price")
            Long maxDealPrice,

            @JsonProperty("count_change_rate")
            Double countChangeRate,

            @JsonProperty("biweekly_trend")
            List<BiweeklyTrendDto> biweeklyTrend,

            @JsonProperty("area_ratio")
            List<AreaRatioDto> areaRatio,

            @JsonProperty("recent_deals")
            List<RecentDealDto> recentDeals,

            @JsonProperty("area_deals")
            List<AreaDealDto> areaDeals
    ){}

    public record BiweeklyTrendDto(
            @JsonProperty("biweekly_period")
            String biweeklyPeriod,

            @JsonProperty("deal_count")
            Integer dealCount,

            @JsonProperty("avg_price")
            Long avgPrice
    ){}

    public record AreaRatioDto(
            @JsonProperty("exclusive_area")
            String exclusiveArea,

            @JsonProperty("pyeong")
            Integer pyeong,

            @JsonProperty("share_percentage")
            Double sharePercentage
    ){}

    public record RecentDealDto(
            @JsonProperty("deal_date")
            String dealDate,

            @JsonProperty("exclusive_area")
            String exclusiveArea,

            @JsonProperty("pyeong")
            Integer pyeong,

            @JsonProperty("floor")
            Integer floor,

            @JsonProperty("deal_amount")
            Long dealAmount
    ){}

    public record AreaDealDto(
            @JsonProperty("exclusive_area")
            String exclusiveArea,

            @JsonProperty("pyeong")
            Integer pyeong,

            @JsonProperty("deal_count")
            Integer dealCount,

            @JsonProperty("avg_deal_price")
            Long avgDealPrice
    ){}
}
