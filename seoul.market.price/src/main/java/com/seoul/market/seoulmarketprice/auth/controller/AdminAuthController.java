package com.seoul.market.seoulmarketprice.auth.controller;

import com.seoul.market.seoulmarketprice.auth.dto.request.AdminLoginRequest;
import com.seoul.market.seoulmarketprice.auth.dto.response.AdminLoginResponse;
import com.seoul.market.seoulmarketprice.auth.dto.response.TokenResponse;
import com.seoul.market.seoulmarketprice.auth.service.AdminAuthService;
import com.seoul.market.seoulmarketprice.auth.service.AdminLoginResult;
import com.seoul.market.seoulmarketprice.auth.service.TokenReissueResult;
import com.seoul.market.seoulmarketprice.security.jwt.AdminTokenCookieManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 인증 요청을 처리하는 Controller이다.
 *
 * <p>
 * 관리자 로그인, Refresh Token Rotation,
 * 로그아웃 요청을 처리한다.
 * </p>
 *
 * <p>
 * 실제 인증 비즈니스 로직은
 * {@link AdminAuthService}에 위임한다.
 * </p>
 */
@Tag(
        name = "관리자 인증",
        description = "관리자 로그인, 토큰 재발급 및 로그아웃 API"
)
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    /**
     * 관리자 인증 비즈니스 로직을 담당한다.
     */
    private final AdminAuthService adminAuthService;

    /**
     * 관리자 Access/Refresh Token 쿠키를 관리한다.
     */
    private final AdminTokenCookieManager
            adminTokenCookieManager;

    /**
     * 생성자 주입을 사용한다.
     *
     * @param adminAuthService       관리자 인증 서비스
     * @param adminTokenCookieManager 관리자 쿠키 관리 객체
     */
    public AdminAuthController(
            AdminAuthService adminAuthService,
            AdminTokenCookieManager adminTokenCookieManager
    ) {
        this.adminAuthService = adminAuthService;
        this.adminTokenCookieManager =
                adminTokenCookieManager;
    }

    /**
     * 관리자 로그인을 처리한다.
     *
     * <p>
     * 로그인 성공 시 Access Token과 Refresh Token을
     * 각각 관리자 전용 쿠키로 전달한다.
     * </p>
     *
     * @param request 관리자 로그인 요청
     * @return Access Token과 관리자 기본 정보
     */
    @Operation(
            summary = "관리자 로그인",
            description = "관리자 아이디와 비밀번호를 검증하고 토큰을 발급한다."
    )
    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(
            @Valid @RequestBody AdminLoginRequest request
    ) {
        // 로그인과 토큰 발급을 처리한다.
        AdminLoginResult result =
                adminAuthService.login(request);

        // 관리자 Access Token 쿠키를 생성한다.
        ResponseCookie accessCookie =
                adminTokenCookieManager
                        .createAccessTokenCookie(
                                result.response()
                                        .accessToken()
                        );

        // 관리자 Refresh Token HttpOnly 쿠키를 생성한다.
        ResponseCookie refreshCookie =
                adminTokenCookieManager
                        .createRefreshTokenCookie(
                                result.rawRefreshToken()
                        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessCookie.toString(),
                        refreshCookie.toString()
                )
                .body(result.response());
    }

    /**
     * 관리자 Access Token과 Refresh Token을 재발급한다.
     *
     * <p>
     * 기존 Refresh Token은 폐기하고,
     * 새로운 Refresh Token을 DB와 쿠키에 저장한다.
     * </p>
     *
     * @param refreshToken 관리자 Refresh Token 쿠키
     * @return 새 관리자 Access Token
     */
    @Operation(
            summary = "관리자 토큰 재발급",
            description = "관리자 Refresh Token Rotation을 수행한다."
    )
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            @CookieValue(
                    name = "adminRefreshToken",
                    required = false
            )
            String refreshToken
    ) {
        if (
                refreshToken == null
                        || refreshToken.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "관리자 Refresh Token이 존재하지 않습니다."
            );
        }

        // 기존 토큰을 폐기하고 새 토큰을 발급한다.
        TokenReissueResult result =
                adminAuthService.reissue(refreshToken);

        // 새 관리자 Access Token 쿠키를 생성한다.
        ResponseCookie accessCookie =
                adminTokenCookieManager
                        .createAccessTokenCookie(
                                result.accessToken()
                        );

        // 새 관리자 Refresh Token 쿠키를 생성한다.
        ResponseCookie refreshCookie =
                adminTokenCookieManager
                        .createRefreshTokenCookie(
                                result.rawRefreshToken()
                        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessCookie.toString(),
                        refreshCookie.toString()
                )
                .body(
                        new TokenResponse(
                                result.accessToken()
                        )
                );
    }

    /**
     * 현재 기기에서 관리자 로그아웃을 처리한다.
     *
     * @param refreshToken 관리자 Refresh Token 쿠키
     * @return 본문 없는 204 응답
     */
    @Operation(
            summary = "관리자 로그아웃",
            description = "관리자 Refresh Token을 폐기하고 인증 쿠키를 삭제한다."
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(
                    name = "adminRefreshToken",
                    required = false
            )
            String refreshToken
    ) {
        // DB에 저장된 관리자 Refresh Token을 폐기한다.
        adminAuthService.logout(refreshToken);

        // 브라우저의 관리자 Access Token 쿠키를 삭제한다.
        ResponseCookie deleteAccessCookie =
                adminTokenCookieManager
                        .deleteAccessTokenCookie();

        // 브라우저의 관리자 Refresh Token 쿠키를 삭제한다.
        ResponseCookie deleteRefreshCookie =
                adminTokenCookieManager
                        .deleteRefreshTokenCookie();

        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        deleteAccessCookie.toString(),
                        deleteRefreshCookie.toString()
                )
                .build();
    }
}