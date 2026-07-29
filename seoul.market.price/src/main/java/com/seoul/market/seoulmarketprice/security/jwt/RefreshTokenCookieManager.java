package com.seoul.market.seoulmarketprice.security.jwt;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Refresh Token 쿠키의 생성과 삭제를 담당한다.
 *
 * <p>
 * JWT 생성과 쿠키 생성을 분리하여
 * 각 클래스가 하나의 책임만 가지도록 설계한다.
 * </p>
 *
 * <p>
 * Refresh Token 쿠키에는 HttpOnly를 적용하여
 * React의 JavaScript 코드에서 직접 읽을 수 없도록 한다.
 * </p>
 */
@Component
public class RefreshTokenCookieManager {

    private final JwtProperties jwtProperties;

    public RefreshTokenCookieManager(
            JwtProperties jwtProperties
    ) {
        this.jwtProperties = jwtProperties;
    }

    public ResponseCookie createRefreshTokenCookie(
            String refreshToken
    ) {
        return ResponseCookie
                .from(
                        jwtProperties.refreshCookieName(),
                        refreshToken
                )
                .httpOnly(true)
                .secure(jwtProperties.cookieSecure())
                .path("/")
                .maxAge(
                        Duration.ofMillis(
                                jwtProperties.refreshTokenExpiry()
                        )
                )
                .sameSite(jwtProperties.cookieSameSite())
                .build();
    }

    /**
     * Refresh Token 쿠키를 삭제한다.
     */
    public ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie
                .from(
                        jwtProperties.refreshCookieName(),
                        ""
                )
                .httpOnly(true)
                .secure(jwtProperties.cookieSecure())
                .path("/")
                .maxAge(0)
                .sameSite(jwtProperties.cookieSameSite())
                .build();
    }
}