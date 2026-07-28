package com.seoul.market.seoulmarketprice.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 JWT 설정값을 자바 객체로 관리한다.
 *
 * <p>
 * @Value를 여러 곳에서 직접 사용하는 대신,
 * JWT 관련 설정을 하나의 record로 묶어 관리한다.
 * </p>
 *
 * <p>
 * record를 사용하므로 Setter 없이 불변 객체로 설정값을 관리할 수 있다.
 * </p>
 *
 * @param secret             JWT 서명에 사용하는 Base64 인코딩 비밀키
 * @param accessTokenExpiry  Access Token 유효시간(밀리초)
 * @param refreshTokenExpiry Refresh Token 유효시간(밀리초)
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpiry,
        long refreshTokenExpiry
) {
}