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
 * OAuth2 로그인 성공 후 JWT를 발급하고
 * 프론트엔드로 리다이렉트하는 성공 처리기이다.
 *
 * <p>
 * 현재 카카오와 구글 로그인에서 공통으로 사용한다.
 * </p>
 *
 * <p>
 * 신규 소셜 회원이면 토큰을 발급하지 않고
 * 프론트엔드로 이동시킨다.
 * 기존 회원이면 Access Token과 Refresh Token을 발급하여
 * 쿠키에 저장한 뒤 프론트엔드로 이동시킨다.
 * </p>
 */
@Component
public class OAuth2SuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    /**
     * OAuth2 로그인 처리 완료 후 이동할 프론트엔드 주소이다.
     */
    private static final String FRONTEND_REDIRECT_URL =
            "http://localhost:3000";

    /**
     * JWT 생성과 검증을 담당한다.
     */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Refresh Token 저장 및 삭제를 담당한다.
     */
    private final RefreshTokenService refreshTokenService;

    /**
     * Refresh Token 쿠키 생성을 담당한다.
     */
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    /**
     * 로그인한 회원을 조회한다.
     */
    private final MemberRepository memberRepository;

    /**
     * JWT 및 쿠키 관련 설정값을 제공한다.
     */
    private final JwtProperties jwtProperties;

    /**
     * 생성자 주입을 사용한다.
     *
     * @param jwtTokenProvider JWT 처리 클래스
     * @param refreshTokenService Refresh Token 서비스
     * @param refreshTokenCookieManager Refresh Token 쿠키 관리자
     * @param memberRepository 회원 Repository
     * @param jwtProperties JWT 설정값
     */
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

    /**
     * OAuth2 인증 성공 후 회원가입 또는 로그인을 처리한다.
     *
     * @param request 현재 HTTP 요청
     * @param response 현재 HTTP 응답
     * @param authentication OAuth2 인증 정보
     * @throws IOException 리다이렉트 또는 응답 처리 실패
     * @throws ServletException 서블릿 처리 실패
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        /*
         * CustomOAuth2UserService에서 생성한
         * OAuth2 공통 사용자 객체를 가져온다.
         */
        CustomOAuth2User oauthUser =
                (CustomOAuth2User)
                        authentication.getPrincipal();

        /*
         * OAuth2 사용자 객체에 저장된 회원 PK를 사용하여
         * 우리 서비스 회원 정보를 조회한다.
         */
        Member member = memberRepository
                .findById(oauthUser.getMemberId())
                .orElseThrow();

        /*
         * 이번 OAuth2 인증으로 새로 가입된 회원이면
         * 토큰을 발급하지 않고 프론트엔드로 이동한다.
         *
         * 현재 구조에서는 회원가입 후 별도로 다시 로그인해야 한다.
         */
        if (oauthUser.isNewMember()) {
            getRedirectStrategy().sendRedirect(
                    request,
                    response,
                    FRONTEND_REDIRECT_URL
            );

            return;
        }

        /*
         * 기존 회원 로그인 시 사용할
         * Access Token을 생성한다.
         */
        String accessToken =
                jwtTokenProvider.createAccessToken(
                        member.getId(),
                        member.getUserId(),
                        Role.USER
                );

        /*
         * Access Token 재발급에 사용할
         * Refresh Token을 생성한다.
         */
        String refreshToken =
                jwtTokenProvider.createRefreshToken(
                        member.getId()
                );

        /*
         * Refresh Token을 DB 또는 저장소에 저장한다.
         */
        refreshTokenService.save(
                member,
                refreshToken
        );

        /*
         * Refresh Token을 담을 HttpOnly 쿠키를 생성한다.
         */
        ResponseCookie refreshCookie =
                refreshTokenCookieManager
                        .createRefreshTokenCookie(
                                refreshToken
                        );

        /*
         * Access Token 쿠키를 생성한다.
         *
         * 현재 일반 로그인 방식과 동일하게
         * 브라우저에서 접근할 수 있도록 httpOnly(false)를 사용한다.
         */
        ResponseCookie accessCookie =
                createAccessTokenCookie(
                        accessToken
                );

        /*
         * Access Token과 Refresh Token을
         * 각각 Set-Cookie 헤더에 추가한다.
         */
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshCookie.toString()
        );

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                accessCookie.toString()
        );

        /*
         * OAuth2 로그인 처리가 완료되면
         * 프론트엔드 메인 주소로 이동한다.
         */
        getRedirectStrategy().sendRedirect(
                request,
                response,
                FRONTEND_REDIRECT_URL
        );
    }

    /**
     * 일반 로그인과 동일한 설정으로
     * Access Token 쿠키를 생성한다.
     *
     * @param accessToken 쿠키에 저장할 Access Token
     * @return Access Token 쿠키
     */
    private ResponseCookie createAccessTokenCookie(
            String accessToken
    ) {
        return ResponseCookie
                .from(
                        "accessToken",
                        accessToken
                )
                .httpOnly(false)
                .secure(
                        jwtProperties.cookieSecure()
                )
                .path("/")
                .maxAge(
                        Duration.ofMillis(
                                jwtProperties
                                        .accessTokenExpiry()
                        )
                )
                .sameSite(
                        jwtProperties.cookieSameSite()
                )
                .build();
    }
}