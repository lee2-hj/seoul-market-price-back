package com.seoul.market.seoulmarketprice.member.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DistrictPreferenceResolverTest {
    @Test
    void usesMyGuBeforeSignupAddress() {
        assertThat(DistrictPreferenceResolver.resolve(
                "강남구", "서울특별시 송파구 올림픽로"
        )).isEqualTo("강남구");
    }

    @Test
    void usesDistrictFromSignupAddressWhenMyGuIsMissing() {
        assertThat(DistrictPreferenceResolver.resolve(
                null, "서울특별시 강동구 성내로 1"
        )).isEqualTo("강동구");
    }

    @Test
    void ignoresInvalidMyGuAndUsesAddress() {
        assertThat(DistrictPreferenceResolver.resolve(
                "경기도", "서울특별시 마포구 월드컵북로"
        )).isEqualTo("마포구");
    }

    @Test
    void defaultsToJungGuWhenNoSeoulDistrictExists() {
        assertThat(DistrictPreferenceResolver.resolve(null, null)).isEqualTo("중구");
        assertThat(DistrictPreferenceResolver.resolve("", "경기도 수원시")).isEqualTo("중구");
    }
}
