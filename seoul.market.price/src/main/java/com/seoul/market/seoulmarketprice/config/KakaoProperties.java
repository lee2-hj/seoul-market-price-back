package com.seoul.market.seoulmarketprice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 카카오 로컬 REST API 연동 설정이다. */
@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(String restApiKey) {
}
