package com.seoul.market.seoulmarketprice.fastapi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record UpdatedAptResponse(
        @JsonProperty("base_date")
        String baseDate, //집계 기준일

        @JsonProperty("compared_base_date")
        String comparedBaseDate, //비교 기준일

        @JsonProperty("count")
        Integer count, //변동 건수

        @JsonProperty("items")
        List<UpdatedAptItemDto> items //변동 목록
) {
    public record UpdatedAptItemDto(
            @JsonProperty("cgg_cd")
            String cggCd, //자치구 코드

            @JsonProperty("cgg_nm")
            String cggNm, //자치구명

            @JsonProperty("stdg_cd")
            String stdgCd, //법정동 코드

            @JsonProperty("stdg_nm")
            String stdgNm, //법정동명

            @JsonProperty("bldg_nm")
            String bldgNm, //건물명

            @JsonProperty("mno")
            String mno, //지번(본번)

            @JsonProperty("sno")
            String sno, //지번(부번)

            @JsonProperty("area")
            Double area, //전용면적

            @JsonProperty("deal_date")
            String dealDate, //거래일

            @JsonProperty("deal_cnt")
            Integer dealCnt, //거래 건수

            @JsonProperty("thing_amt")
            Long thingAmt, //거래 금액(만원)

            @JsonProperty("pyeong_amt")
            Long pyeongAmt, //평단가(만원)

            @JsonProperty("latitude")
            Double latitude, //위도

            @JsonProperty("longitude")
            Double longitude, //경도

            @JsonProperty("status")
            String status //변동 상태(NEW, UPDATED 등)
    ){}
}
