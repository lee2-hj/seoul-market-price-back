package com.seoul.market.seoulmarketprice.auth.controller;

import com.seoul.market.seoulmarketprice.auth.dto.request.AdminLoginRequest;
import com.seoul.market.seoulmarketprice.auth.dto.response.AdminLoginResponse;
import com.seoul.market.seoulmarketprice.auth.service.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 인증 요청을 처리하는 Controller이다.
 *
 * <p>
 * 관리자 로그인 요청을 받고,
 * 실제 인증 처리는 AdminAuthService에 위임한다.
 * </p>
 */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(
            AdminAuthService adminAuthService
    ) {
        this.adminAuthService = adminAuthService;
    }

    /**
     * 관리자 로그인을 처리한다.
     *
     * <p>
     * 요청 주소:
     * POST /api/admin/auth/login
     * </p>
     *
     * @param request 관리자 아이디와 비밀번호
     * @return Access Token과 관리자 정보
     */
    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(
            @Valid @RequestBody AdminLoginRequest request
    ) {
        AdminLoginResponse response =
                adminAuthService.login(request);

        return ResponseEntity.ok(response);
    }
}