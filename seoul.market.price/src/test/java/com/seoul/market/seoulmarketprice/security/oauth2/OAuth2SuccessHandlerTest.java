package com.seoul.market.seoulmarketprice.security.oauth2;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.auth.repository.MemberRepository;
import com.seoul.market.seoulmarketprice.config.FrontendProperties;
import com.seoul.market.seoulmarketprice.security.jwt.JwtProperties;
import com.seoul.market.seoulmarketprice.security.jwt.JwtTokenProvider;
import com.seoul.market.seoulmarketprice.security.jwt.RefreshTokenCookieManager;
import com.seoul.market.seoulmarketprice.token.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * OAuth2SuccessHandler가 실제로 Set-Cookie 헤더를 응답에 담는지 검증한다.
 *
 * <p>
 * 카카오 로그인 후 브라우저에 토큰 쿠키가 심어지지 않는다는 신고가 있어,
 * response.sendRedirect() 호출 이후에도 앞서 addHeader로 추가한
 * Set-Cookie 헤더가 그대로 남아있는지 확인한다.
 * </p>
 */
class OAuth2SuccessHandlerTest {

    @Test
    void 기존_회원이면_응답에_AccessToken과_RefreshToken_쿠키가_모두_담긴다() throws Exception {

        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        RefreshTokenCookieManager refreshTokenCookieManager = mock(RefreshTokenCookieManager.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        JwtProperties jwtProperties = new JwtProperties(
                "seoul-market-price-jwt-secret-key-backend-project-team1234",
                1800000L,
                1209600000L,
                "refreshToken",
                "adminAccessToken",
                "adminRefreshToken",
                false,
                "Lax"
        );

        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(member.getUserId()).thenReturn("kakao_123456789");
        when(member.getName()).thenReturn("홍길동");

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(jwtTokenProvider.createAccessToken(1L, "kakao_123456789", com.seoul.market.seoulmarketprice.auth.entity.Role.USER))
                .thenReturn("access.token.value");
        when(jwtTokenProvider.createRefreshToken(1L))
                .thenReturn("refresh.token.value");
        when(refreshTokenCookieManager.createRefreshTokenCookie("refresh.token.value"))
                .thenReturn(
                        ResponseCookie.from("refreshToken", "refresh.token.value")
                                .httpOnly(true)
                                .secure(false)
                                .path("/")
                                .maxAge(Duration.ofMillis(1209600000L))
                                .sameSite("Lax")
                                .build()
                );

        FrontendProperties frontendRedirectProperties =
                new FrontendProperties(
                        "localhost:3000/login",
                        "localhost:3000"
                );

        OAuth2SuccessHandler handler = new OAuth2SuccessHandler(
                jwtTokenProvider,
                refreshTokenService,
                refreshTokenCookieManager,
                memberRepository,
                jwtProperties,
                frontendRedirectProperties
        );

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("memberExists", true);
        attributes.put("memberId", 1L);
        attributes.put("userId", "kakao_123456789");

        CustomOAuth2User principal = new CustomOAuth2User(attributes);
        Authentication authentication = new TestingAuthenticationToken(principal, null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/kakao");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        java.util.List<String> setCookieHeaders = response.getHeaders("Set-Cookie");

        assertThat(setCookieHeaders)
                .as("리다이렉트 이후에도 Set-Cookie 헤더가 응답에 남아있어야 한다")
                .anyMatch(header -> header.startsWith("accessToken="))
                .anyMatch(header -> header.startsWith("refreshToken="));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:3000");
    }

    @Test
    void 가입되지_않은_회원이면_쿠키_없이_alert_후_로그인_페이지로_이동하는_HTML을_내려준다() throws Exception {

        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        RefreshTokenCookieManager refreshTokenCookieManager = mock(RefreshTokenCookieManager.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        JwtProperties jwtProperties = new JwtProperties(
                "seoul-market-price-jwt-secret-key-backend-project-team1234",
                1800000L,
                1209600000L,
                "refreshToken",
                "adminAccessToken",
                "adminRefreshToken",
                false,
                "Lax"
        );

        FrontendProperties frontendRedirectProperties =
                new FrontendProperties(
                        "localhost:3000/login",
                        "localhost:3000"
                );

        OAuth2SuccessHandler handler = new OAuth2SuccessHandler(
                jwtTokenProvider,
                refreshTokenService,
                refreshTokenCookieManager,
                memberRepository,
                jwtProperties,
                frontendRedirectProperties
        );

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("memberExists", false);

        CustomOAuth2User principal = new CustomOAuth2User(attributes);
        Authentication authentication = new TestingAuthenticationToken(principal, null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/kakao");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getHeaders("Set-Cookie"))
                .as("가입되지 않은 회원에게는 쿠키를 발급하지 않아야 한다")
                .isEmpty();

        assertThat(response.getRedirectedUrl())
                .as("302 리다이렉트가 아니라 alert 스크립트를 담은 200 응답이어야 한다")
                .isNull();

        String body = response.getContentAsString();

        assertThat(body)
                .contains("alert('존재하지 않는 회원입니다.')")
                .contains("location.replace('http://localhost:3000/login')")
                .doesNotContain("error=member_not_found");
    }

    @Test
    void 회원가입_시도인데_이미_가입된_회원이면_쿠키_없이_중복_안내_후_로그인_페이지로_이동한다() throws Exception {

        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        RefreshTokenCookieManager refreshTokenCookieManager = mock(RefreshTokenCookieManager.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        JwtProperties jwtProperties = new JwtProperties(
                "seoul-market-price-jwt-secret-key-backend-project-team1234",
                1800000L,
                1209600000L,
                "refreshToken",
                "adminAccessToken",
                "adminRefreshToken",
                false,
                "Lax"
        );

        FrontendProperties frontendRedirectProperties =
                new FrontendProperties(
                        "localhost:3000/login",
                        "localhost:3000"
                );

        OAuth2SuccessHandler handler = new OAuth2SuccessHandler(
                jwtTokenProvider,
                refreshTokenService,
                refreshTokenCookieManager,
                memberRepository,
                jwtProperties,
                frontendRedirectProperties
        );

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("memberExists", true);
        attributes.put("isSignupFlow", true);
        attributes.put("memberId", 1L);
        attributes.put("userId", "kakao_123456789");

        CustomOAuth2User principal = new CustomOAuth2User(attributes);
        Authentication authentication = new TestingAuthenticationToken(principal, null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/kakao-signup");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getHeaders("Set-Cookie"))
                .as("이미 가입된 회원의 회원가입 시도에는 쿠키를 발급하지 않아야 한다")
                .isEmpty();

        assertThat(response.getRedirectedUrl()).isNull();

        assertThat(response.getContentAsString())
                .contains("alert('이미 존재한 회원입니다 로그인을 진행해주세요')")
                .contains("location.replace('http://localhost:3000/login')");
    }

    @Test
    void 회원가입_시도이고_신규_회원이면_쿠키_없이_가입_완료_안내_후_로그인_페이지로_이동한다() throws Exception {

        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        RefreshTokenCookieManager refreshTokenCookieManager = mock(RefreshTokenCookieManager.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        JwtProperties jwtProperties = new JwtProperties(
                "seoul-market-price-jwt-secret-key-backend-project-team1234",
                1800000L,
                1209600000L,
                "refreshToken",
                "adminAccessToken",
                "adminRefreshToken",
                false,
                "Lax"
        );

        FrontendProperties frontendRedirectProperties =
                new FrontendProperties(
                        "localhost:3000/login",
                        "localhost:3000"
                );

        OAuth2SuccessHandler handler = new OAuth2SuccessHandler(
                jwtTokenProvider,
                refreshTokenService,
                refreshTokenCookieManager,
                memberRepository,
                jwtProperties,
                frontendRedirectProperties
        );

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("memberExists", false);
        attributes.put("isSignupFlow", true);
        attributes.put("memberId", 2L);
        attributes.put("userId", "kakao_987654321");

        CustomOAuth2User principal = new CustomOAuth2User(attributes);
        Authentication authentication = new TestingAuthenticationToken(principal, null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/kakao-signup");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getHeaders("Set-Cookie"))
                .as("회원가입은 자동 로그인하지 않으므로 쿠키를 발급하지 않아야 한다")
                .isEmpty();

        assertThat(response.getRedirectedUrl()).isNull();

        assertThat(response.getContentAsString())
                .contains("alert('회원 가입이 완료 되었습니다.')")
                .contains("location.replace('http://localhost:3000/login')");
    }
}
