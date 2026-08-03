package com.seoul.market.seoulmarketprice.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 JWT 및 토큰 쿠키 설정을 관리한다.
 *
 * <p>
 * 일반 회원과 관리자의 쿠키 이름을 분리하여
 * 두 인증 정보가 서로 덮어쓰이지 않도록 한다.
 * </p>
 *
 * @param secret                 JWT 서명 및 검증에 사용하는 비밀키
 * @param accessTokenExpiry      Access Token 만료 시간(밀리초)
 * @param refreshTokenExpiry     Refresh Token 만료 시간(밀리초)
 * @param refreshCookieName      일반 회원 Refresh Token 쿠키 이름
 * @param adminAccessCookieName  관리자 Access Token 쿠키 이름
 * @param adminRefreshCookieName 관리자 Refresh Token 쿠키 이름
 * @param cookieSecure           HTTPS에서만 쿠키를 전달할지 여부
 * @param cookieSameSite         쿠키의 SameSite 정책
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(

        String secret,

        long accessTokenExpiry,

        long refreshTokenExpiry,

        String refreshCookieName,

        String adminAccessCookieName,

        String adminRefreshCookieName,

        boolean cookieSecure,

        String cookieSameSite

) {
}