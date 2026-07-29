package com.seoul.market.seoulmarketprice.security.oauth2;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.auth.repository.MemberRepository;
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

/**
 * OAuth2 로그인 성공 후 JWT를 발급한다.
 */
@Component
public class OAuth2SuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;
    private final MemberRepository memberRepository;

    public OAuth2SuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            RefreshTokenCookieManager refreshTokenCookieManager,
            MemberRepository memberRepository
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenCookieManager = refreshTokenCookieManager;
        this.memberRepository = memberRepository;
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

        // Access Token 생성
        String accessToken =
                jwtTokenProvider.createAccessToken(
                        member.getId(),
                        member.getUserId()
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
        ResponseCookie cookie =
                refreshTokenCookieManager.createRefreshTokenCookie(
                        refreshToken
                );

        // Refresh Token을 쿠키로 전달
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );

        // 카카오 로그인 완료 후 React로 이동
        getRedirectStrategy().sendRedirect(
                request,
                response,
                "http://localhost:5173/oauth2/success?accessToken="
                        + accessToken
        );
    }
}