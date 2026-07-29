package com.seoul.market.seoulmarketprice.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 JWT 및 Refresh Token 쿠키 설정을 관리한다.
 *
 * <p>
 * 설정값을 여러 클래스에 하드코딩하지 않고
 * 하나의 불변 객체로 관리하기 위해 record를 사용한다.
 * </p>
 *
 * @param secret             JWT 서명 및 검증에 사용하는 Base64 비밀키
 * @param accessTokenExpiry  Access Token 만료 시간(밀리초)
 * @param refreshTokenExpiry Refresh Token 만료 시간(밀리초)
 * @param refreshCookieName  Refresh Token을 저장할 쿠키 이름
 * @param cookieSecure       HTTPS 환경에서만 쿠키를 전달할지 여부
 * @param cookieSameSite     쿠키의 SameSite 정책
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpiry,
        long refreshTokenExpiry,
        String refreshCookieName,
        boolean cookieSecure,
        String cookieSameSite
) {
}