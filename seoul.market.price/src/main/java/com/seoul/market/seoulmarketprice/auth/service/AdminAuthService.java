package com.seoul.market.seoulmarketprice.auth.service;

import com.seoul.market.seoulmarketprice.auth.dto.request.AdminLoginRequest;

/**
 * 관리자 인증 기능을 정의하는 서비스 인터페이스이다.
 *
 * <p>
 * 관리자 로그인, Refresh Token Rotation,
 * 로그아웃 기능을 제공한다.
 * </p>
 */
public interface AdminAuthService {

    /**
     * 관리자 로그인을 처리한다.
     *
     * @param request 관리자 로그인 요청
     * @return 로그인 응답과 관리자 Refresh Token 원문
     */
    AdminLoginResult login(AdminLoginRequest request);

    /**
     * 관리자 Refresh Token Rotation을 처리한다.
     *
     * @param rawRefreshToken 기존 관리자 Refresh Token 원문
     * @return 새 Access Token과 새 Refresh Token
     */
    TokenReissueResult reissue(String rawRefreshToken);

    /**
     * 현재 관리자의 Refresh Token을 폐기한다.
     *
     * @param rawRefreshToken 폐기할 관리자 Refresh Token
     */
    void logout(String rawRefreshToken);
}