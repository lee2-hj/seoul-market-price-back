package com.seoul.market.seoulmarketprice.security.jwt;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 관리자 Refresh Token 쿠키의 생성과 삭제를 담당한다.
 *
 * <p>
 * 일반 회원의 {@link RefreshTokenCookieManager}와 동일한 역할이며,
 * 쿠키 이름만 관리자 전용 이름을 사용해 일반 회원과 관리자의
 * 인증 정보가 서로 덮어쓰이지 않도록 한다.
 * </p>
 *
 * <p>
 * Access Token 쿠키는 {@link RefreshTokenCookieManager}를 사용하는
 * {@link com.seoul.market.seoulmarketprice.auth.controller.AuthController}와
 * 동일하게, 관리자 쪽도
 * {@link com.seoul.market.seoulmarketprice.auth.controller.AdminAuthController}가
 * 직접 생성한다.
 * </p>
 */
@Component
public class AdminTokenCookieManager {

    /**
     * JWT 만료시간과 쿠키 설정값을 제공한다.
     */
    private final JwtProperties jwtProperties;

    /**
     * 생성자 주입을 사용한다.
     *
     * @param jwtProperties JWT 및 쿠키 설정
     */
    public AdminTokenCookieManager(
            JwtProperties jwtProperties
    ) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 관리자 Refresh Token 쿠키를 생성한다.
     *
     * <p>
     * JavaScript에서 Refresh Token을 읽을 수 없도록
     * HttpOnly를 적용한다.
     * </p>
     *
     * @param refreshToken 관리자 Refresh Token 원문
     * @return 관리자 Refresh Token 쿠키
     */
    public ResponseCookie createRefreshTokenCookie(
            String refreshToken
    ) {
        return ResponseCookie
                .from(
                        jwtProperties.adminRefreshCookieName(),
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
     * 관리자 Refresh Token 삭제 쿠키를 생성한다.
     *
     * @return 만료시간이 0인 관리자 Refresh Token 쿠키
     */
    public ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie
                .from(
                        jwtProperties.adminRefreshCookieName(),
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