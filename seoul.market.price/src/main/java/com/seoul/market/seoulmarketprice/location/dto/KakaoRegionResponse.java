package com.seoul.market.seoulmarketprice.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** 카카오 좌표→행정구역 API 응답 중 필요한 필드만 매핑한다. */
public record KakaoRegionResponse(List<Document> documents) {
    public record Document(
            @JsonProperty("region_type") String regionType,
            @JsonProperty("region_1depth_name") String region1DepthName,
            @JsonProperty("region_2depth_name") String region2DepthName,
            @JsonProperty("code") String code
        ) {
    }
}
