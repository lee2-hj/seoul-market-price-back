package com.seoul.market.seoulmarketprice.auth.service;

/**
 * 토큰 재발급 결과를 Service와 Controller 사이에서 전달하는 내부 객체이다.
 *
 * <p>
 * Access Token은 응답 본문으로 전달하고,
 * Refresh Token은 HttpOnly 쿠키로 전달하기 위해 두 값을 함께 보관한다.
 * </p>
 *
 * @param accessToken     새로 발급한 Access Token
 * @param rawRefreshToken 새로 발급한 Refresh Token 원문
 */
public record TokenReissueResult(
        String accessToken,
        String rawRefreshToken
) {
}