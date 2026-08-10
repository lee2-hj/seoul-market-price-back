package com.seoul.market.seoulmarketprice.security.oauth2;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.auth.entity.Role;
import com.seoul.market.seoulmarketprice.auth.repository.MemberRepository;
import com.seoul.market.seoulmarketprice.config.FrontendProperties;
import com.seoul.market.seoulmarketprice.security.jwt.JwtProperties;
import com.seoul.market.seoulmarketprice.security.jwt.JwtTokenProvider;
import com.seoul.market.seoulmarketprice.security.jwt.RefreshTokenCookieManager;
import com.seoul.market.seoulmarketprice.token.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;

/**
 * OAuth2 로그인 성공 후 JWT를 발급하고
 * 프론트엔드로 리다이렉트하는 성공 처리기이다.
 *
 * <p>
 * 현재 카카오와 구글 로그인/회원가입에서 공통으로 사용한다.
 * </p>
 *
 * <p>
 * 일반 회원가입/로그인이 분리된 것과 동일하게, 소셜 로그인도
 * 로그인과 회원가입의 역할을 완전히 분리한다.
 * </p>
 *
 * <ul>
 * <li>로그인 + 가입된 회원: Access Token과 Refresh Token을
 * 발급하여 쿠키에 저장한 뒤 프론트엔드 루트 페이지로 이동시킨다.</li>
 * <li>로그인 + 가입되지 않은 회원: 토큰을 발급하지 않고
 * "존재하지 않는 회원입니다." 알림을 띄운 뒤 로그인 페이지로
 * 이동시킨다.</li>
 * <li>회원가입 + 이미 가입된 회원(중복): 토큰을 발급하지 않고
 * "이미 존재한 회원입니다 로그인을 진행해주세요" 알림을 띄운 뒤
 * 로그인 페이지로 이동시킨다.</li>
 * <li>회원가입 + 신규 회원: CustomOAuth2UserService가 이미 회원을
 * 생성했으므로, 토큰은 발급하지 않고 "회원 가입이 완료
 * 되었습니다." 알림을 띄운 뒤 로그인 페이지로 이동시킨다
 * (자동 로그인하지 않는다).</li>
 * </ul>
 */
@Component
public class OAuth2SuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    /**
     * 어느 리다이렉트 경로를 탔는지, 쿠키를 실제로 발급했는지 기록한다.
     */
    private static final Logger log =
            LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    /**
     * .env.local(LOGIN_PAGE, ROOT_PAGE)에는 스킴이 빠진
     * host[:port][/path] 형태로 값을 적어두므로, URL로 사용할 때
     * 이 스킴을 붙인다.
     *
     * 운영 환경에서 HTTPS로 서비스한다면 이 값도 함께 바꿔야 한다.
     */
    private static final String FRONTEND_SCHEME = "http://";

    /**
     * OAuth2 로그인 처리 완료 후 이동할 프론트엔드 주소이다.
     */
    private final String frontendRootPageUrl;

    /**
     * 가입되지 않은 회원이 소셜 로그인을 시도했을 때
     * 이동할 프론트엔드 로그인 페이지 주소이다.
     */
    private final String frontendLoginPageUrl;

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
     * @param frontendRedirectProperties 프론트엔드 리다이렉트 주소 설정값
     */
    public OAuth2SuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            RefreshTokenCookieManager refreshTokenCookieManager,
            MemberRepository memberRepository,
            JwtProperties jwtProperties,
            FrontendProperties frontendRedirectProperties
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenCookieManager = refreshTokenCookieManager;
        this.memberRepository = memberRepository;
        this.jwtProperties = jwtProperties;

        this.frontendRootPageUrl =
                FRONTEND_SCHEME + frontendRedirectProperties.rootPage();

        this.frontendLoginPageUrl =
                FRONTEND_SCHEME + frontendRedirectProperties.loginPage();
    }

    /**
     * OAuth2 인증 성공 후 로그인 또는 회원가입을 처리한다.
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
         * OAuth2 인가 코드 교환 과정에만 필요했던 세션을 정리한다.
         *
         * SecurityConfig의 sessionCreationPolicy가 IF_REQUIRED이므로,
         * 정리하지 않으면 Spring Security가 방금 로그인에 성공한
         * OAuth2AuthenticationToken(principal = CustomOAuth2User)을
         * HTTP 세션에 저장해버린다. 이후 브라우저가 그 세션 쿠키를
         * API 요청에 함께 보내면, JwtAuthenticationFilter가 실행되기
         * 전에 SecurityContext가 이미 채워져 있어 필터가 동작하지
         * 않고, @AuthenticationPrincipal CustomUserPrincipal이
         * 타입 불일치로 null이 주입되어 NPE가 발생한다.
         *
         * 로그인 이후에는 JWT만으로 인증하므로 세션 기반 인증 정보는
         * 즉시 제거해도 된다.
         */
        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        boolean isSignupFlow = oauthUser.isSignupFlow();
        boolean memberExists = oauthUser.memberExists();

        /*
         * 회원가입은 로그인 처리를 하지 않는다(자동 로그인 없음).
         * 결과만 alert으로 안내하고 로그인 페이지로 보낸다.
         *
         * 302 리다이렉트로는 브라우저 alert을 띄울 수 없으므로,
         * alert() 후 location을 옮기는 스크립트를 담은 HTML을
         * 직접 응답으로 내려준다.
         */
        if (isSignupFlow) {

            if (memberExists) {

                log.info(
                        "이미 가입된 회원의 소셜 회원가입 시도로 {}(으)로 이동합니다.",
                        frontendLoginPageUrl
                );

                respondWithAlertAndRedirect(
                        response,
                        "이미 존재한 회원입니다 로그인을 진행해주세요",
                        frontendLoginPageUrl
                );

            } else {

                log.info(
                        "소셜 회원가입 완료로 {}(으)로 이동합니다.",
                        frontendLoginPageUrl
                );

                respondWithAlertAndRedirect(
                        response,
                        "회원 가입이 완료 되었습니다.",
                        frontendLoginPageUrl
                );
            }

            return;
        }

        /*
         * 로그인 시도인데 가입된 회원이 아니면 회원가입을 대신
         * 만들지 않고, 토큰 발급 없이 브라우저에 알림을 띄운 뒤
         * 로그인 페이지로 이동한다.
         */
        if (!memberExists) {

            log.info(
                    "가입되지 않은 소셜 회원 로그인 시도로 쿠키를 발급하지 않고 "
                            + "{}(으)로 이동합니다.",
                    frontendLoginPageUrl
            );

            respondWithAlertAndRedirect(
                    response,
                    "존재하지 않는 회원입니다.",
                    frontendLoginPageUrl
            );

            return;
        }

        /*
         * 로그인 + 가입된 회원인 경우이다.
         * CustomOAuth2UserService가 memberId를 attributes에
         * 채워 넣었으므로, 이를 이용해 서비스 회원 정보를 조회한다.
         */
        Member member = memberRepository
                .findById(oauthUser.getMemberId())
                .orElseThrow();

        /*
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
         * 이 흐름은 302 리다이렉트로 끝나 응답 본문으로
         * Access Token을 전달할 수 없다. 따라서 일반/관리자
         * 로그인과 달리 프론트엔드가 쿠키를 직접 읽어
         * Authorization 헤더를 구성할 수 있도록
         * httpOnly(false)를 그대로 유지한다.
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

        log.info(
                "memberId={} 로그인 성공, Access/Refresh Token 쿠키를 발급하고 {}(으)로 이동합니다.",
                member.getId(),
                frontendRootPageUrl
        );

        /*
         * OAuth2 로그인 처리가 완료되면
         * 프론트엔드 메인 주소로 이동한다.
         */
        getRedirectStrategy().sendRedirect(
                request,
                response,
                frontendRootPageUrl
        );
    }

    /**
     * 브라우저에 alert 창을 띄운 뒤 지정한 주소로 이동시킨다.
     *
     * <p>
     * 302 리다이렉트 응답은 자바스크립트를 실행하지 않으므로,
     * alert() 호출과 location 이동을 담은 최소한의 HTML을
     * 200 응답으로 직접 내려준다.
     * </p>
     *
     * @param response 현재 HTTP 응답
     * @param message  alert 창에 표시할 문구
     * @param redirectUrl 이동할 주소
     * @throws IOException 응답 작성 실패
     */
    private void respondWithAlertAndRedirect(
            HttpServletResponse response,
            String message,
            String redirectUrl
    ) throws IOException {

        response.setContentType("text/html;charset=UTF-8");

        String escapedMessage =
                message.replace("\\", "\\\\").replace("'", "\\'");

        try (PrintWriter writer = response.getWriter()) {
            writer.write(
                    "<!DOCTYPE html><html><body><script>"
                            + "alert('" + escapedMessage + "');"
                            + "location.replace('" + redirectUrl + "');"
                            + "</script></body></html>"
            );
        }
    }

    /**
     * OAuth2 로그인 리다이렉트 흐름에서 사용할
     * Access Token 쿠키를 생성한다.
     *
     * <p>
     * 응답 본문을 전달할 수 없는 리다이렉트 흐름이므로
     * 일반/관리자 로그인과 달리 HttpOnly를 적용하지 않는다.
     * </p>
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