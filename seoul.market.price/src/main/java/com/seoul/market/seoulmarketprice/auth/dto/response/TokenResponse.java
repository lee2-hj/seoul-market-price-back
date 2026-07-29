package com.seoul.market.seoulmarketprice.auth.dto.response;

/**
 * Access Token 재발급 응답 DTO이다.
 *
 * <p>
 * Refresh Token은 HttpOnly 쿠키로 전달하므로
 * 응답 본문에는 Access Token만 포함한다.
 * </p>
 *
 * @param accessToken 새로 발급한 Access Token
 */
public record TokenResponse(
        String accessToken
) {
}