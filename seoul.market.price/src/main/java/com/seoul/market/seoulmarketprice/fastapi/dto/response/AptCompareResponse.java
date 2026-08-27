package com.seoul.market.seoulmarketprice.fastapi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.JoinColumn;

public record AptCompareResponse(
        @JsonProperty("base_date")
        String baseDate,

        @JsonProperty("cgg_cd")
        String cggCd,

        @JsonProperty("cgg_nm")
        String cggNm,

        @JsonProperty("stdg_cd")
        String stdgCd,

        @JsonProperty("stdg_nm")
        String stdgNm,

        @JsonProperty("bldg_nm")
        String bldgNm,

        @JsonProperty("grp")
        groupDto grp,

        @JsonProperty("grp2")
        groupDto grp2
) {

    public record groupDto(

            @JsonProperty("pyeong_grp")
            String pyeongGrp,

            @JsonProperty("flr_grp")
            String floorGrp,

            @JsonProperty("deal_cnt")
            Integer dealCnt,

            @JsonProperty("avg_thing_amt")
            Long avgThingAmt,

            @JsonProperty("avg_pyeong_amt")
            Long avgPyeongAmt,

            @JsonProperty("recent_thing_amt")
            Long recentThingAmt,

            @JsonProperty("recent_pyeong_amt")
            Long recentPyeongAmt,

            @JsonProperty("recent_deal_date")
            String recentDealDate,

            @JsonProperty("recent_supply_pyeong")
            Integer recentSupplyPyeong,

            @JsonProperty("recent_floor")
            Integer recentFloor
    ){}
}
