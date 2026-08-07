package com.seoul.market.seoulmarketprice.auth.controller;

import com.seoul.market.seoulmarketprice.auth.dto.request.LoginRequest;
import com.seoul.market.seoulmarketprice.auth.dto.response.LoginResponse;
import com.seoul.market.seoulmarketprice.auth.dto.response.TokenResponse;
import com.seoul.market.seoulmarketprice.auth.service.AuthService;
import com.seoul.market.seoulmarketprice.auth.service.LoginResult;
import com.seoul.market.seoulmarketprice.auth.service.TokenReissueResult;
import com.seoul.market.seoulmarketprice.security.jwt.JwtProperties;
import com.seoul.market.seoulmarketprice.security.jwt.RefreshTokenCookieManager;
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
 * 인증 관련 요청을 처리하는 Controller.
 */
@Tag(name = "로그인 및 인증", description = "로그인 및 인증에 관한 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /**
     * 인증 서비스.
     */
    private final AuthService authService;

    /**
     * Refresh Token 쿠키 생성.
     */
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    /**
     * Access Token 쿠키 생성에 필요한 만료 시간, 보안 옵션을 제공한다.
     */
    private final JwtProperties jwtProperties;

    /**
     * Access Token을 브라우저 쿠키에 직접 심어주기 위한 쿠키를 생성한다.
     *
     * <p>
     * Refresh Token과 달리 프론트엔드에서 Authorization 헤더 구성에
     * 사용할 수 있어야 하므로 HttpOnly를 적용하지 않는다.
     * </p>
     */
    private ResponseCookie createAccessTokenCookie(String accessToken) {
        return ResponseCookie
                .from("accessToken", accessToken)
                .httpOnly(false)
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

//    public AuthController(
//            AuthService authService,
//            RefreshTokenCookieManager refreshTokenCookieManager
//    ) {
//        this.authService = authService;
//        this.refreshTokenCookieManager = refreshTokenCookieManager;
//    }

    /**
     * 일반 로그인.
     */
    @Operation(summary = "일반 로그인", description = "아이디와 비밀번호를 사용하여 로그인한다.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        // 로그인 처리
        LoginResult result = authService.login(request);

        // Refresh Token 쿠키 생성
        ResponseCookie refreshCookie =
                refreshTokenCookieManager.createRefreshTokenCookie(
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
     * Access Token 재발급.
     */
    @Operation(summary = "Access Token 재발급", description = "Refresh Token을 사용하여 Access Token을 재발급한다.")
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(

            @CookieValue(
                    name = "refreshToken",
                    required = false
            )
            String refreshToken

    ) {

        // Refresh Token 확인
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Refresh Token이 존재하지 않습니다."
            );
        }

        // 토큰 재발급
        TokenReissueResult result =
                authService.reissue(refreshToken);

        // 새 Refresh Token 쿠키 생성
        ResponseCookie refreshCookie =
                refreshTokenCookieManager.createRefreshTokenCookie(
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
     * 현재 기기에서 로그아웃한다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(
                    name = "refreshToken",
                    required = false
            )
            String refreshToken
    ) {
        // DB의 Refresh Token 폐기
        authService.logout(refreshToken);

        // 브라우저의 Refresh Token 쿠키 삭제
        ResponseCookie deleteRefreshCookie =
                refreshTokenCookieManager.deleteRefreshTokenCookie();

        // 브라우저의 Access Token 쿠키 삭제
        ResponseCookie deleteAccessCookie =
                ResponseCookie
                        .from("accessToken", "")
                        .httpOnly(false)
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
