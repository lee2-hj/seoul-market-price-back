package com.seoul.market.seoulmarketprice.security.jwt;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 관리자 Access Token과 Refresh Token 쿠키의
 * 생성 및 삭제를 담당하는 클래스이다.
 *
 * <p>
 * 일반 회원의 토큰 쿠키와 이름을 분리하여
 * 관리자와 일반 회원의 인증 정보가 서로
 * 덮어쓰이지 않도록 한다.
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
     * 관리자 Access Token 쿠키를 생성한다.
     *
     * <p>
     * 관리자 Access Token은 응답 본문으로도 함께 전달되므로,
     * 프론트엔드는 쿠키를 직접 읽지 않고 응답 본문의 값을
     * Authorization 헤더 구성에 사용한다. 따라서 Refresh Token과
     * 동일하게 HttpOnly를 적용해 JavaScript로 쿠키를 읽지 못하게 한다.
     * </p>
     *
     * @param accessToken 관리자 Access Token
     * @return 관리자 Access Token 쿠키
     */
    public ResponseCookie createAccessTokenCookie(
            String accessToken
    ) {
        return ResponseCookie
                .from(
                        jwtProperties.adminAccessCookieName(),
                        accessToken
                )
                .httpOnly(true)
                .secure(jwtProperties.cookieSecure())
                .path("/")
                .maxAge(
                        Duration.ofMillis(
                                jwtProperties.accessTokenExpiry()
                        )
                )
                .sameSite(jwtProperties.cookieSameSite())
                .build();
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
     * 관리자 Access Token 삭제 쿠키를 생성한다.
     *
     * @return 만료시간이 0인 관리자 Access Token 쿠키
     */
    public ResponseCookie deleteAccessTokenCookie() {
        return ResponseCookie
                .from(
                        jwtProperties.adminAccessCookieName(),
                        ""
                )
                .httpOnly(true)
                .secure(jwtProperties.cookieSecure())
                .path("/")
                .maxAge(0)
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