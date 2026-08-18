package com.seoul.market.seoulmarketprice.fastapi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TopAndBottomResponse(

        @JsonProperty("base_date")
        String baseDate,

        @JsonProperty("total_count")
        Integer totalCount,

        @JsonProperty("avg_thing_amt")
        Long avgThingAmt,

        @JsonProperty("avg_pyeong_amt")
        Long avgPyeongAmt,

        @JsonProperty("top")
        List<BldgDealSummaryDto> top,

        @JsonProperty("bottom")
        List<BldgDealSummaryDto> bottom

) {
    public record BldgDealSummaryDto(
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

            @JsonProperty("latitude")
            Double latitude,

            @JsonProperty("longitude")
            Double longitude,

            @JsonProperty("is_exact_location")
            Boolean isExactLocation,

            @JsonProperty("updated_at")
            String updatedAt,

            @JsonProperty("deal_cnt")
            Integer dealCnt,

            @JsonProperty("total_thing_amt")
            Long totalThingAmt,

            @JsonProperty("total_pyeong_amt")
            Long totalPyeongAmt
    ){}
}
