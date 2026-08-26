package com.seoul.market.seoulmarketprice.location.service;

import com.seoul.market.seoulmarketprice.location.client.KakaoRegionClient;
import com.seoul.market.seoulmarketprice.location.dto.KakaoRegionResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocationServiceTest {
    private final KakaoRegionClient client = mock(KakaoRegionClient.class);
    private final LocationService service = new LocationService(client);

    @Test
    void returnsSeoulDistrictFromAdministrativeRegion() {
        when(client.getRegion(37.5301, 127.1238)).thenReturn(
                new KakaoRegionResponse(List.of(
                        new KakaoRegionResponse.Document("B", "서울특별시", "강동구", "1174010100"),
                        new KakaoRegionResponse.Document("H", "서울특별시", "강동구", "1174069000")
                ))
        );

        assertThat(service.findCurrentDistrict(37.5301, 127.1238).district())
                .isEqualTo("강동구");
        assertThat(service.findCurrentDistrict(37.5301, 127.1238).sggCd()).isEqualTo("11740");
    }

    @Test
    void rejectsLocationOutsideSeoul() {
        when(client.getRegion(37.4563, 126.7052)).thenReturn(
                new KakaoRegionResponse(List.of(
                        new KakaoRegionResponse.Document("H", "인천광역시", "남동구", "2820010100")
                ))
        );

        assertThatThrownBy(() -> service.findCurrentDistrict(37.4563, 126.7052))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("현재 위치가 서울 지역이 아닙니다.");
    }

    @Test
    void rejectsInvalidCoordinatesBeforeCallingKakao() {
        assertThatThrownBy(() -> service.findCurrentDistrict(91, 127))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("위도 또는 경도 값이 올바르지 않습니다.");
    }
}
