package com.seoul.market.seoulmarketprice.fastapi.service;

import com.seoul.market.seoulmarketprice.fastapi.dto.request.CompareRequest;
import com.seoul.market.seoulmarketprice.fastapi.dto.request.ListRequest;
import com.seoul.market.seoulmarketprice.fastapi.dto.request.TopAndBottomRequest;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.CompareResponse;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.ListResponse;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.TopAndBottomResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FastApiService {

    private final RestClient restClient;

    public CompareResponse getCompare(CompareRequest request) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/compare/dong-pyeong")
                        .queryParam("region1_cgg_cd", request.guCode1())
                        .queryParam("region1_stdg_cd", request.dongCode1())
                        .queryParam("region2_cgg_cd", request.guCode2())
                        .queryParam("region2_stdg_cd", request.dongCode2())
                        .build())
                .retrieve()
                .body(CompareResponse.class);
    }

    public ListResponse getPyeongList(@Valid ListRequest request) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/dong/list")
                        .queryParam("region_cgg", request.guCode())
                        .build())
                .retrieve()
                .body(ListResponse.class);
    }

    public TopAndBottomResponse getTopAndBottom(@Valid TopAndBottomRequest request) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/apt-price/top-bottom")
                        .queryParam("region_cgg_cd", request.guCode())
                        .queryParam("region_stdg_cd", request.dongCode())
                        .build())
                .retrieve()
                .body(TopAndBottomResponse.class);
    }
}
