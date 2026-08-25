package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.location.client.KakaoPlaceClient;
import com.seoul.market.seoulmarketprice.location.dto.KakaoPlaceResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KakaoPlaceResolverTest {
    private final KakaoPlaceClient client = mock(KakaoPlaceClient.class);
    private final KakaoPlaceResolver resolver = new KakaoPlaceResolver(client);

    @Test
    void resolvesSingleSeoulStationToCoordinates() {
        when(client.search("홍대입구역", "STATION")).thenReturn(new KakaoPlaceResponse(List.of(
                document("1", "홍대입구역", "서울특별시 마포구 동교동", "서울특별시 마포구 양화로 160",
                        "126.925381", "37.557192")
        )));

        var result = resolver.resolve("홍대입구역", "STATION");

        assertThat(result.status()).isEqualTo("RESOLVED");
        assertThat(result.resolvedPlace().name()).isEqualTo("홍대입구역");
        assertThat(result.resolvedPlace().latitude()).isEqualTo(37.557192);
        assertThat(result.resolvedPlace().longitude()).isEqualTo(126.925381);
        assertThat(result.resolvedPlace().source()).isEqualTo("KAKAO");
    }

    @Test
    void asksForClarificationWhenMultipleSeoulPlacesMatch() {
        when(client.search("시청역", "STATION")).thenReturn(new KakaoPlaceResponse(List.of(
                document("1", "시청역 1호선", "서울특별시 중구", "", "126.977", "37.565"),
                document("2", "시청역 2호선", "서울특별시 중구", "", "126.976", "37.564")
        )));

        var result = resolver.resolve("시청역", "STATION");

        assertThat(result.status()).isEqualTo("NEED_CLARIFICATION");
        assertThat(result.candidates()).hasSize(2);
    }

    @Test
    void excludesPlacesOutsideSeoul() {
        when(client.search("시청역", "STATION")).thenReturn(new KakaoPlaceResponse(List.of(
                document("3", "시청역", "부산광역시 연제구", "", "129.079", "35.180")
        )));

        assertThat(resolver.resolve("시청역", "STATION").status()).isEqualTo("NOT_FOUND");
    }

    private KakaoPlaceResponse.Document document(String id, String name, String address, String roadAddress,
                                                   String x, String y) {
        return new KakaoPlaceResponse.Document(id, name, "SW8", "교통,수송 > 지하철,전철",
                address, roadAddress, x, y);
    }
}
