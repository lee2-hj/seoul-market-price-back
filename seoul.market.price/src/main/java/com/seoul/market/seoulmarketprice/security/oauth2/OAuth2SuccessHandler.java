package com.seoul.market.seoulmarketprice.security.oauth2;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.auth.entity.Role;
import com.seoul.market.seoulmarketprice.auth.repository.MemberRepository;
import com.seoul.market.seoulmarketprice.security.jwt.JwtProperties;
import com.seoul.market.seoulmarketprice.security.jwt.JwtTokenProvider;
import com.seoul.market.seoulmarketprice.security.jwt.RefreshTokenCookieManager;
import com.seoul.market.seoulmarketprice.token.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/**
 * OAuth2 로그인 성공 후 JWT를 발급하고 프론트엔드로 리다이렉트한다.
 *
 * 신규 회원이면 카카오 회원가입으로 처리하며 토큰 없이 프론트로 리다이렉트하고,
 * 기존 회원이면 카카오 로그인으로 처리하며 쿠키에 토큰을 심은 뒤 프론트로 리다이렉트한다.
 */
@Component
public class OAuth2SuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private static final String FRONTEND_REDIRECT_URL = "http://localhost:3000";

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;
    private final MemberRepository memberRepository;
    private final JwtProperties jwtProperties;

    public OAuth2SuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            RefreshTokenCookieManager refreshTokenCookieManager,
            MemberRepository memberRepository,
            JwtProperties jwtProperties
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenCookieManager = refreshTokenCookieManager;
        this.memberRepository = memberRepository;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public void onAuthenticationSuccess(

            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication

    ) throws IOException, ServletException {

        // OAuth2 사용자 정보
        KakaoOAuth2User oauthUser =
                (KakaoOAuth2User) authentication.getPrincipal();

        // 회원 조회
        Member member = memberRepository
                .findById(oauthUser.getMemberId())
                .orElseThrow();

        if (oauthUser.isNewMember()) {
            // 카카오 회원가입: 토큰은 발급하지 않고 쿠키도 심지 않은 채 프론트로 리다이렉트한다.
            // 로그인은 별도로 다시 시도해야 한다.
            getRedirectStrategy().sendRedirect(
                    request,
                    response,
                    FRONTEND_REDIRECT_URL
            );
            return;
        }

        // Access Token 생성
        String accessToken =
                jwtTokenProvider.createAccessToken(
                        member.getId(),
                        member.getUserId(),
                        Role.USER
                );

        // Refresh Token 생성
        String refreshToken =
                jwtTokenProvider.createRefreshToken(
                        member.getId()
                );

        // Refresh Token 저장
        refreshTokenService.save(
                member,
                refreshToken
        );

        // Refresh Token 쿠키 생성
        ResponseCookie refreshCookie =
                refreshTokenCookieManager.createRefreshTokenCookie(
                        refreshToken
                );

        // Access Token 쿠키 생성 (일반 로그인과 동일한 설정)
        ResponseCookie accessCookie = createAccessTokenCookie(accessToken);

        // Access Token, Refresh Token 모두 쿠키로 전달
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshCookie.toString()
        );
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                accessCookie.toString()
        );

        // 카카오 로그인 완료: 쿠키에 토큰을 심은 뒤 프론트로 리다이렉트한다.
        getRedirectStrategy().sendRedirect(
                request,
                response,
                FRONTEND_REDIRECT_URL
        );
    }

    /**
     * 일반 로그인(AuthController)과 동일한 설정으로 Access Token 쿠키를 만든다.
     */
    private ResponseCookie createAccessTokenCookie(String accessToken) {
        return ResponseCookie
                .from("accessToken", accessToken)
                .httpOnly(false)
                .secure(jwtProperties.cookieSecure())
                .path("/")
                .maxAge(Duration.ofMillis(jwtProperties.accessTokenExpiry()))
                .sameSite(jwtProperties.cookieSameSite())
                .build();
    }
}