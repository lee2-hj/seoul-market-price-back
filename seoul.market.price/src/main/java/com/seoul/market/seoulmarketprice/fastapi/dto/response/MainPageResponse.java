package com.seoul.market.seoulmarketprice.fastapi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MainPageResponse(

        @JsonProperty("cgg_cd")
        String cggCd,

        @JsonProperty("period_start")
        String periodStart,

        @JsonProperty("period_end")
        String periodEnd,

        @JsonProperty("seoul_top5_districts")
        List<DistrictDto> seoulTop5Districts,

        @JsonProperty("price_change_top5")
        PriceChangeTop5Dto priceChangeTop5,

        @JsonProperty("preference_price_trend")
        List<PriceTrendDto> preferencePriceTrend,

        @JsonProperty("preference_top_trading_dongs")
        List<TradingDongDto> preferenceTopTradingDongs,

        @JsonProperty("preference_popular_dong")
        PopularDongDto preferencePopularDong,

        @JsonProperty("preference_top_trading_apts")
        List<TradingAptDto> preferenceTopTradingApts

) {
    public record DistrictDto(
            @JsonProperty("cgg_nm")
            String cggNm,

            @JsonProperty("avg_deal_price")
            Long avgDealPrice,

            @JsonProperty("avg_pyeong_price")
            Long avgPyeongPrice
    ){}

    public record PriceChangeTop5Dto(
            @JsonProperty("rising_top5")
            List<ChangeRateDto> risingTop5,

            @JsonProperty("falling_top5")
            List<ChangeRateDto> fallingTop5
    ){}

    public record ChangeRateDto(
            @JsonProperty("bldg_nm")
            String bldgNm,

            @JsonProperty("change_rate")
            Double changeRate
    ){}

    public record PriceTrendDto(
            @JsonProperty("period_label")
            String periodLabel,

            @JsonProperty("start_date")
            String startDate,

            @JsonProperty("end_date")
            String endDate,

            @JsonProperty("avg_deal_price")
            Long avgDealPrice,

            @JsonProperty("avg_pyeong_price")
            Long avgPyeongPrice,

            @JsonProperty("deal_cnt")
            Integer dealCnt
    ){}

    public record TradingDongDto(
            @JsonProperty("cgg_nm")
            String cggNm,

            @JsonProperty("stdg_nm")
            String stdgNm,

            @JsonProperty("deal_cnt")
            Integer dealCnt
    ){}

    public record PopularDongDto(
            @JsonProperty("cgg_nm")
            String cggNm,

            @JsonProperty("stdg_nm")
            String stdgNm
    ){}

    public record TradingAptDto(
            @JsonProperty("bldg_nm")
            String bldgNm,

            @JsonProperty("recent_thing_amt")
            Long recentThingAmt,

            @JsonProperty("deal_cnt")
            Integer dealCnt
    ){}
}
