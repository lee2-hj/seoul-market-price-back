package com.seoul.market.seoulmarketprice.location.client;

import com.seoul.market.seoulmarketprice.config.KakaoProperties;
import com.seoul.market.seoulmarketprice.location.dto.KakaoPlaceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class KakaoPlaceClient {
    private static final Logger log = LoggerFactory.getLogger(KakaoPlaceClient.class);
    private static final String BASE_URL = "https://dapi.kakao.com";
    private final RestClient restClient;

    public KakaoPlaceClient(KakaoProperties properties) {
        String apiKey = properties.restApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("KAKAO_REST_API_KEY 환경변수가 설정되지 않았습니다.");
        }
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey.trim())
                .build();
    }

    public KakaoPlaceResponse search(String query, String placeType) {
        try {
            KakaoPlaceResponse response = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/v2/local/search/keyword.json")
                                .queryParam("query", query)
                                .queryParam("size", 10);
                        if ("STATION".equals(placeType)) {
                            uriBuilder.queryParam("category_group_code", "SW8");
                        }
                        return uriBuilder.build();
                    })
                    .retrieve().body(KakaoPlaceResponse.class);
            if (response == null) throw new IllegalStateException("카카오 장소 검색 응답이 비어 있습니다.");
            return response;
        } catch (RestClientResponseException exception) {
            log.warn("카카오 장소 검색 실패: status={}", exception.getStatusCode());
            if (exception.getStatusCode().is4xxClientError()) {
                throw new IllegalStateException("카카오 REST API 키 또는 장소 요청을 확인해 주세요.");
            }
            throw new IllegalStateException("카카오 장소 API와 통신 중 오류가 발생했습니다.");
        }
    }
}
