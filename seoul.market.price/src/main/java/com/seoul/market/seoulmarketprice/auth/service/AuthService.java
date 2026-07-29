package com.seoul.market.seoulmarketprice.auth.service;

import com.seoul.market.seoulmarketprice.auth.dto.request.LoginRequest;

/**
 * 인증 관련 비즈니스 로직을 정의하는 서비스.
 */
public interface AuthService {

    /**
     * 일반 로그인.
     */
    LoginResult login(LoginRequest request);

    /**
     * Access Token과 Refresh Token 재발급.
     */
    TokenReissueResult reissue(String rawRefreshToken);

    /**
     * 현재 Refresh Token 폐기.
     */
    void logout(String rawRefreshToken);
}