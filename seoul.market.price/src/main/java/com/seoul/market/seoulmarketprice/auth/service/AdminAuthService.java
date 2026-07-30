package com.seoul.market.seoulmarketprice.auth.service;

import com.seoul.market.seoulmarketprice.auth.dto.request.AdminLoginRequest;
import com.seoul.market.seoulmarketprice.auth.dto.response.AdminLoginResponse;

/**
 * 관리자 인증 기능을 정의하는 서비스 인터페이스이다.
 *
 * <p>
 * 관리자 로그인 요청을 받아 아이디와 비밀번호를 검증하고,
 * 로그인 성공 시 관리자용 Access Token을 반환한다.
 * </p>
 */
public interface AdminAuthService {

    /**
     * 관리자 로그인을 처리한다.
     *
     * @param request 관리자 로그인 요청 정보
     * @return Access Token과 관리자 기본 정보
     */
    AdminLoginResponse login(AdminLoginRequest request);
}