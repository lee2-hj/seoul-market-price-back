package com.seoul.market.seoulmarketprice.location.client;

import com.seoul.market.seoulmarketprice.config.KakaoProperties;
import com.seoul.market.seoulmarketprice.location.dto.KakaoRegionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** 카카오 로컬 API로 좌표의 행정구역을 조회한다. */
@Component
public class KakaoRegionClient {
    private static final Logger log = LoggerFactory.getLogger(KakaoRegionClient.class);
    private static final String BASE_URL = "https://dapi.kakao.com";

    private final RestClient restClient;

    public KakaoRegionClient(KakaoProperties properties) {
        String apiKey = properties.restApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "KAKAO_REST_API_KEY 환경변수가 설정되지 않았습니다."
            );
        }
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey.trim())
                .build();
    }

    public KakaoRegionResponse getRegion(double latitude, double longitude) {
        try {
            KakaoRegionResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/geo/coord2regioncode.json")
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .build())
                    .retrieve()
                    .body(KakaoRegionResponse.class);
            if (response == null) {
                throw new IllegalStateException("카카오 위치 API 응답이 비어 있습니다.");
            }
            return response;
        } catch (RestClientResponseException exception) {
            log.warn("카카오 행정구역 조회 실패: status={}", exception.getStatusCode());
            if (exception.getStatusCode().is4xxClientError()) {
                throw new IllegalStateException(
                        "카카오 REST API 키 또는 좌표 요청을 확인해 주세요."
                );
            }
            throw new IllegalStateException(
                    "카카오 위치 API와 통신 중 오류가 발생했습니다."
            );
        }
    }
}
