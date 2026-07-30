package com.seoul.market.seoulmarketprice.auth.controller;

import com.seoul.market.seoulmarketprice.auth.dto.request.LoginRequest;
import com.seoul.market.seoulmarketprice.auth.dto.response.LoginResponse;
import com.seoul.market.seoulmarketprice.auth.dto.response.TokenResponse;
import com.seoul.market.seoulmarketprice.auth.service.AuthService;
import com.seoul.market.seoulmarketprice.auth.service.LoginResult;
import com.seoul.market.seoulmarketprice.auth.service.TokenReissueResult;
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
        ResponseCookie cookie =
                refreshTokenCookieManager.createRefreshTokenCookie(
                        result.rawRefreshToken()
                );

        // Access Token은 본문, Refresh Token은 쿠키로 전달
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookie.toString()
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
        ResponseCookie cookie =
                refreshTokenCookieManager.createRefreshTokenCookie(
                        result.rawRefreshToken()
                );

        // Access Token은 본문, Refresh Token은 쿠키로 전달
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookie.toString()
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
        ResponseCookie deleteCookie =
                refreshTokenCookieManager.deleteRefreshTokenCookie();

        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        deleteCookie.toString()
                )
                .build();
    }

}
