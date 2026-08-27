package com.seoul.market.seoulmarketprice.fastapi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RttRespopnse(
        @JsonProperty("sgg_cd")
        String sggCd,

        @JsonProperty("sgg_nm")
        String sggNm,

        @JsonProperty("dong_cd")
        String dongCd,

        @JsonProperty("dong_nm")
        String dongNm,

        @JsonProperty("period_start")
        String periodStart,

        @JsonProperty("period_end")
        String periodEnd,

        @JsonProperty("total_deal_cnt")
        Integer totalDealCnt,

        @JsonProperty("total_trade_amount")
        Long totalTradeAmount,

        @JsonProperty("avg_trade_amount")
        Long avgTradeAmount,

        @JsonProperty("max_trade_amount")
        Long maxTradeAmount,

        @JsonProperty("volume_change_rate")
        Double volumeChangeRate,

        @JsonProperty("biweekly_trend")
        List<BiweeklyTrendDto> biweeklyTrend,

        @JsonProperty("pyeong_distribution")
        List<PyeongDistributionDto> pyeongDistribution,

        @JsonProperty("recent_trades")
        List<RecentTradeDto> recentTrades,

        @JsonProperty("top5_by_volume")
        List<Top5ByVolumeDto> top5ByVolume
) {
    public record BiweeklyTrendDto(
            @JsonProperty("period_label")
            String periodLabel,

            @JsonProperty("start_date")
            String startDate,

            @JsonProperty("end_date")
            String endDate,

            @JsonProperty("deal_cnt")
            Integer dealCnt,

            @JsonProperty("avg_trade_amount")
            Long avgTradeAmount
    ){}

    public record PyeongDistributionDto(
            @JsonProperty("pyeong_grp")
            String pyeongGrp,

            @JsonProperty("deal_cnt")
            Integer dealCnt,

            @JsonProperty("ratio")
            Double ratio
    ){}

    public record RecentTradeDto(
            @JsonProperty("apt_name")
            String aptName,

            @JsonProperty("mno")
            String mno,

            @JsonProperty("sno")
            String sno,

            @JsonProperty("deal_date")
            String dealDate,

            @JsonProperty("floor")
            Integer floor,

            @JsonProperty("trade_amount")
            Long tradeAmount,

            @JsonProperty("pyeong")
            Double pyeong,

            @JsonProperty("exclusive_area_m2")
            Double exclusiveAreaM2
    ){}

    public record Top5ByVolumeDto(
            @JsonProperty("apt_name")
            String aptName,

            @JsonProperty("mno")
            String mno,

            @JsonProperty("sno")
            String sno,

            @JsonProperty("deal_cnt")
            Integer dealCnt,

            @JsonProperty("avg_trade_amount")
            Long avgTradeAmount
    ){}
}
