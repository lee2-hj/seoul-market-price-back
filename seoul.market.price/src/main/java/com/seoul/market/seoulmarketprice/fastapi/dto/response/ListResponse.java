package com.seoul.market.seoulmarketprice.fastapi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record ListResponse(
        @JsonProperty("base_date")
        String baseDate,

        @JsonProperty("groups")
        Map<String, ListSummaryDto> groups
) {
    public record ListSummaryDto(
            @JsonProperty("code")
            String code,

            @JsonProperty("name")
            String name,

            @JsonProperty("total_count")
            Integer total_count,

            @JsonProperty("avg_thing_amt")
            Long avg_thing_amt,

            @JsonProperty("avg_pyeong_amt")
            Long avg_pyeong_amt
    ){}
}
