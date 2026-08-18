package com.seoul.market.seoulmarketprice.fastapi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CompareResponse(
        @JsonProperty("base_date")
        String baseDate, //집계일

        @JsonProperty("region1")
        RegionSummaryDto region1, //1지역

        @JsonProperty("region2")
        RegionSummaryDto region2 //2지역
) {
    // 1. 지역별 요약 메타 record
    public record RegionSummaryDto(
            @JsonProperty("cgg_cd")
            String cggCd, //자치구 코드

            @JsonProperty("stdg_cd")
            String stdgCd, //법정동 코드

            @JsonProperty("total_count")
            Long totalCount, //총 거래량

            @JsonProperty("avg_thing_amt")
            Long avgThingAmt, //평균 매매가

            @JsonProperty("avg_pyeong_amt")
            Long avgPyeongAmt//평균 평단가
    ){}
}
