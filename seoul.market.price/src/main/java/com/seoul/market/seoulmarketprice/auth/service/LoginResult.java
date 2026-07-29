package com.seoul.market.seoulmarketprice.auth.service;

import com.seoul.market.seoulmarketprice.auth.dto.response.LoginResponse;

/**
 * 로그인 처리 결과를 Service와 Controller 사이에서 전달하는 내부 객체이다.
 *
 * <p>
 * 클라이언트에게 직접 반환하는 API 응답 DTO는 아니다.
 * Controller가 응답 본문과 Refresh Token 쿠키를 각각 만들 수 있도록
 * 두 값을 함께 전달한다.
 * </p>
 *
 * @param response        React에 응답 본문으로 전달할 로그인 정보
 * @param rawRefreshToken HttpOnly 쿠키에 저장할 Refresh Token 원문
 */
public record LoginResult(
        LoginResponse response,
        String rawRefreshToken
) {
}