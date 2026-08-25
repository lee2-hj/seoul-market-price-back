package com.seoul.market.seoulmarketprice.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** 카카오 키워드 장소 검색 응답 중 도구 실행에 필요한 값만 매핑한다. */
public record KakaoPlaceResponse(List<Document> documents) {
    public record Document(
            String id,
            @JsonProperty("place_name") String placeName,
            @JsonProperty("category_group_code") String categoryGroupCode,
            @JsonProperty("category_name") String categoryName,
            @JsonProperty("address_name") String addressName,
            @JsonProperty("road_address_name") String roadAddressName,
            String x,
            String y
    ) {}
}
