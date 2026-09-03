package com.seoul.market.seoulmarketprice.auth.controller;

import com.seoul.market.seoulmarketprice.auth.dto.request.AdminLoginRequest;
import com.seoul.market.seoulmarketprice.auth.dto.response.AdminLoginResponse;
import com.seoul.market.seoulmarketprice.auth.dto.response.TokenResponse;
import com.seoul.market.seoulmarketprice.auth.service.AdminAuthService;
import com.seoul.market.seoulmarketprice.auth.service.AdminLoginResult;
import com.seoul.market.seoulmarketprice.auth.service.TokenReissueResult;
import com.seoul.market.seoulmarketprice.security.jwt.AdminTokenCookieManager;
import com.seoul.market.seoulmarketprice.security.jwt.JwtProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

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
 *
 * <p>
 * 토큰 발급/쿠키 구성 로직은 {@link AuthController}와
 * 동일한 방식(Refresh Token은 전용 매니저, Access Token은
 * 응답 본문과 동일한 값을 컨트롤러에서 직접 쿠키로 구성)을 따른다.
 * </p>
 */
@Tag(
        name = "관리자 인증",
        description = "관리자 로그인, 토큰 재발급 및 로그아웃 API"
)
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    /**
     * 관리자 인증 비즈니스 로직을 담당한다.
     */
    private final AdminAuthService adminAuthService;

    /**
     * 관리자 Refresh Token 쿠키 생성을 담당한다.
     */
    private final AdminTokenCookieManager adminTokenCookieManager;

    /**
     * Access Token 쿠키 생성에 필요한 만료 시간, 보안 옵션을 제공한다.
     */
    private final JwtProperties jwtProperties;

    /**
     * 관리자 Access Token을 브라우저 쿠키에 심어주기 위한 쿠키를 생성한다.
     *
     * <p>
     * Access Token은 응답 본문으로도 함께 전달되므로,
     * 프론트엔드는 쿠키를 직접 읽지 않고 응답 본문의 값을
     * Authorization 헤더 구성에 사용한다. 따라서 Refresh Token과
     * 동일하게 HttpOnly를 적용해 JavaScript로 쿠키를 읽지 못하게 한다.
     * </p>
     *
     * <p>
     * {@link AuthController#createAccessTokenCookie(String)}와 동일한
     * 로직이며, 쿠키 이름만 관리자 전용 이름을 사용한다.
     * </p>
     */
    private ResponseCookie createAccessTokenCookie(String accessToken) {
        return ResponseCookie
                .from(jwtProperties.adminAccessCookieName(), accessToken)
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
        // 로그인 처리
        AdminLoginResult result = adminAuthService.login(request);

        // Refresh Token 쿠키 생성
        ResponseCookie refreshCookie =
                adminTokenCookieManager.createRefreshTokenCookie(
                        result.rawRefreshToken()
                );

        // Access Token 쿠키 생성
        ResponseCookie accessCookie =
                createAccessTokenCookie(
                        result.response().accessToken()
                );

        // Access Token, Refresh Token 모두 쿠키로 전달
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString(),
                        accessCookie.toString()
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
        // Refresh Token 확인
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException(
                    "관리자 Refresh Token이 존재하지 않습니다."
            );
        }

        // 토큰 재발급
        TokenReissueResult result =
                adminAuthService.reissue(refreshToken);

        // 새 Refresh Token 쿠키 생성
        ResponseCookie refreshCookie =
                adminTokenCookieManager.createRefreshTokenCookie(
                        result.rawRefreshToken()
                );

        // 새 Access Token 쿠키 생성
        ResponseCookie accessCookie =
                createAccessTokenCookie(
                        result.accessToken()
                );

        // Access Token, Refresh Token 모두 쿠키로 전달
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString(),
                        accessCookie.toString()
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
        // DB의 Refresh Token 폐기
        adminAuthService.logout(refreshToken);

        // 브라우저의 Refresh Token 쿠키 삭제
        ResponseCookie deleteRefreshCookie =
                adminTokenCookieManager.deleteRefreshTokenCookie();

        // 브라우저의 Access Token 쿠키 삭제
        ResponseCookie deleteAccessCookie =
                ResponseCookie
                        .from(jwtProperties.adminAccessCookieName(), "")
                        .httpOnly(true)
                        .secure(jwtProperties.cookieSecure())
                        .path("/")
                        .maxAge(0)
                        .sameSite(jwtProperties.cookieSameSite())
                        .build();

        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        deleteRefreshCookie.toString(),
                        deleteAccessCookie.toString()
                )
                .build();
    }

}
