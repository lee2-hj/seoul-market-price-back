package com.seoul.market.seoulmarketprice.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoul.market.seoulmarketprice.ai.filter.AiSearchRateLimitFilter;
import com.seoul.market.seoulmarketprice.common.dto.ErrorResponse;
import com.seoul.market.seoulmarketprice.security.jwt.JwtAuthenticationFilter;
import com.seoul.market.seoulmarketprice.security.oauth2.CustomOAuth2UserService;
import com.seoul.market.seoulmarketprice.security.oauth2.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;

/**
 * Spring Security 설정.
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 인증/인가 실패 응답 본문을 JSON으로 작성할 때 사용한다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UrlBasedCorsConfigurationSource corsConfigurationSource,
            ObjectMapper objectMapper,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AiSearchRateLimitFilter aiSearchRateLimitFilter,
            CustomOAuth2UserService customOAuth2UserService,
            OAuth2SuccessHandler oauth2SuccessHandler,
            @Value("${app.admin.creation-public:false}")
            boolean adminCreationPublic
    ) throws Exception {

        http
                // JWT 방식이므로 CSRF와 세션을 사용하지 않는다.
                .csrf(csrf -> csrf.disable())

                // CORS 설정을 적용한다.
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource
                ))

                // 일반 JWT 인증에는 세션을 사용하지 않지만 OAuth2 인가 과정에는 필요 시 세션을 허용한다.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                // Spring 기본 로그인 방식을 사용하지 않는다.
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                /*
                 * 인증/인가 실패 응답을 명시적으로 구분한다.
                 *
                 * AuthenticationEntryPoint/AccessDeniedHandler를 등록하지
                 * 않으면 Spring Security는 로그인 여부와 무관하게 모든
                 * 실패를 기본 Http403ForbiddenEntryPoint로 처리해
                 * 토큰이 없거나 만료된 경우까지 403으로 응답한다.
                 * 그러면 프론트엔드가 401 응답에만 반응하는 세션 만료
                 * 처리(재로그인 유도)가 전혀 동작하지 않는다.
                 *
                 * - 인증 자체가 안 된 경우(토큰 없음/무효/만료): 401
                 * - 인증은 됐지만 권한이 부족한 경우(USER가 ADMIN API 접근 등): 403
                 */
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) ->
                                writeErrorResponse(
                                        response,
                                        HttpStatus.UNAUTHORIZED,
                                        String.valueOf(HttpStatus.UNAUTHORIZED.value()),
                                        "로그인이 필요합니다."
                                )
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeErrorResponse(
                                        response,
                                        HttpStatus.FORBIDDEN,
                                        String.valueOf(HttpStatus.FORBIDDEN.value()),
                                        "접근 권한이 없습니다."
                                )
                        )
                )

                // 요청별 접근 권한을 설정한다.
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                                "/api/auth/login",
                                "/api/auth/reissue",
                                "/api/auth/logout",
                                "/api/admin/auth/login",
                                "/api/admin/auth/reissue",
                                "/api/admin/auth/logout",
                                "/api/members",
                                "/api/members/signup",
                                "/api/members/check-user-id",
                                "/api/members/check-member",
                                "/api/members/check-id",
                                "/api/members/find-id",
                                "/api/members/password-reset/**",
                                "/api/location/current-district",
                                // 로그인 전 회원가입 화면에서도 지역 목록을 조회할 수 있도록 공개한다.
                                "/api/location/sggs",
                                "/api/location/dongs",
                                "/api/page-views",

                                // 회원가입 화면(로그인 전)에서 호출하는
                                // 휴대폰 PASS 본인인증 결과 확인 API
                                "/api/members/phone-verification/**",

                                "/oauth2/**",
                                "/login/oauth2/**",

                                // fastApi 호출 API는 로그인 없이도 접근할 수 있도록 공개한다.
                                "/fastApi/**",

                                // 엘라스틱서치 검색 API는 로그인 없이도 접근할 수 있도록 공개한다.
                                "/elasticSearch/**",

                                // Swagger UI 및 OpenAPI 명세 조회는 인증 없이 접근 허용
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll();

                    // 메인 AI 검색은 비로그인 사용자에게도 공개하되, IP별 요청 제한 필터를 적용한다.
                    auth.requestMatchers(HttpMethod.POST, "/api/ai/search-natural")
                            .permitAll();

                    /*
                     * 개발 환경에서는 최초 관리자 생성을 위해 임시 공개한다.
                     * 운영 환경에서 app.admin.creation-public=false로 설정하면
                     * 아래 permitAll 규칙이 등록되지 않는다.
                     */
                    if (adminCreationPublic) {
                        auth.requestMatchers(
                                HttpMethod.POST,
                                "/api/admins"
                        ).permitAll();
                    }

                    auth.requestMatchers(HttpMethod.GET, "/api/qnas/me")
                            .hasRole("USER");
                    auth.requestMatchers(HttpMethod.GET, "/api/comments/me")
                            .hasRole("USER");
                    // 공개 Q&A와 그 하위 첨부파일 조회 경로를 비로그인 사용자에게 허용한다.
                    auth.requestMatchers(HttpMethod.GET, "/api/qnas", "/api/qnas/**")
                            .permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/qnas")
                            .hasRole("USER");
                    auth.requestMatchers(HttpMethod.PATCH, "/api/qnas/**")
                            .hasRole("USER");
                    auth.requestMatchers(HttpMethod.DELETE, "/api/qnas/**")
                            .hasRole("USER");
                    auth.requestMatchers(HttpMethod.GET, "/api/boards/me")
                            .hasRole("USER");
                    auth.requestMatchers(HttpMethod.GET, "/api/boards", "/api/boards/**")
                            .permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/faqs", "/api/faqs/**")
                            .permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/boards", "/api/boards/**")
                            .hasRole("USER");
                    auth.requestMatchers(HttpMethod.PATCH, "/api/boards/**")
                            .hasRole("USER");
                    auth.requestMatchers(HttpMethod.DELETE, "/api/boards/**")
                            .hasRole("USER");

                    /*
                     * 활성 메뉴: /me는 ADMIN·MASTER만 호출할 수 있다(role에 따라 반환 범위가 다름).
                     * {id} 조회(GET)는 원래 동작대로 role과 무관하게 로그인한 사용자면 누구나
                     * 호출할 수 있다. 등록/해제(POST/DELETE)는 관리 기능이므로 ADMIN·MASTER만
                     * 허용하고, ADMIN이 자기 자신이 아닌 다른 관리자를 대상으로 하지 못하도록
                     * 막는 것은 서비스 레벨 IDOR 검증(ActiveMenuService)의 몫이다.
                     * (/me가 /api/activeMenu/** 보다 먼저 선언되어야 우선 적용된다.)
                     */
                    auth.requestMatchers(HttpMethod.GET, "/api/activeMenu/me")
                            .hasAnyRole("ADMIN", "MASTER");
                    auth.requestMatchers(HttpMethod.GET, "/api/activeMenu/**")
                            .authenticated();
                    auth.requestMatchers(HttpMethod.POST, "/api/activeMenu/**")
                            .hasAnyRole("ADMIN", "MASTER");
                    auth.requestMatchers(HttpMethod.DELETE, "/api/activeMenu/**")
                            .hasAnyRole("ADMIN", "MASTER");

                    // 메뉴/메뉴 카테고리 카탈로그는 조회·등록·수정·삭제 모두 ADMIN·MASTER 둘 다 허용한다.
                    auth.requestMatchers(HttpMethod.GET,
                                    "/api/menus", "/api/menus/**",
                                    "/api/menuCategory", "/api/menuCategory/**")
                            .hasAnyRole("ADMIN", "MASTER");
                    auth.requestMatchers(HttpMethod.POST,
                                    "/api/menus", "/api/menus/**",
                                    "/api/menuCategory", "/api/menuCategory/**")
                            .hasAnyRole("ADMIN", "MASTER");
                    auth.requestMatchers(HttpMethod.PATCH, "/api/menus/**", "/api/menuCategory/**")
                            .hasAnyRole("ADMIN", "MASTER");
                    auth.requestMatchers(HttpMethod.DELETE, "/api/menus/**", "/api/menuCategory/**")
                            .hasAnyRole("ADMIN", "MASTER");

                    /*
                     * 운영 환경의 관리자 생성 및 관리자 전용 API는 ADMIN 또는 MASTER 권한이 필요하다.
                     * 역할 계층(RoleHierarchy)이 별도로 설정되어 있지 않으므로
                     * hasRole("ADMIN")만으로는 MASTER 계정이 접근할 수 없어 hasAnyRole로 확장한다.
                     */
                    auth.requestMatchers(
                                    "/api/admin/**",
                                    "/api/admins",
                                    "/api/admins/**"
                            )
                            .hasAnyRole("ADMIN", "MASTER")
                            // 그 외 요청은 로그인한 사용자만 접근할 수 있다.
                            .anyRequest().authenticated();
                })

                // 카카오 OAuth2 로그인을 설정한다.
                .oauth2Login(oauth2 -> oauth2

                        /*
                         * 카카오 토큰 발급 요청에 사용할 클라이언트를 지정한다.
                         * 기본 클라이언트는 classpath의 Apache HttpClient5를 자동으로
                         * 사용하는데, 이 클라이언트는 429(Too Many Requests) 응답을
                         * 받으면 1초 뒤 자동으로 재요청을 보낸다. 카카오의 요청 제한은
                         * 1초 만에 풀리지 않으므로 재요청도 동일하게 429로 실패하며,
                         * 결과적으로 요청 한 번에 카카오 서버로 두 번 호출이 나가
                         * 제한 초과 상태를 더 오래 유지시킨다. JDK HttpClient 기반
                         * 요청 팩토리는 상태 코드로 자동 재시도를 하지 않으므로
                         * 이 문제를 방지한다.
                         */
                        .tokenEndpoint(token ->
                                token.accessTokenResponseClient(
                                        kakaoAccessTokenResponseClient()
                                )
                        )

                        // 카카오 사용자 조회 및 회원 저장 처리
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(
                                        customOAuth2UserService
                                )
                        )

                        // 로그인 성공 후 JWT 발급 처리
                        .successHandler(oauth2SuccessHandler)

                        /*
                         * 실패 원인을 로그로 남긴다. 기본 실패 핸들러는 예외를
                         * DEBUG 레벨로만 남기기 때문에 카카오 쪽 거절 사유
                         * (예: invalid_grant, unauthorized_client 등)를
                         * 운영 로그에서 확인할 수 없었다.
                         */
                        .failureHandler((request, response, exception) -> {
                            log.error("소셜 로그인 실패: {}", exception.getMessage(), exception);
                            new SimpleUrlAuthenticationFailureHandler("/login?error")
                                    .onAuthenticationFailure(request, response, exception);
                        })
                )

                // Access Token 인증 필터를 등록한다.
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .addFilterBefore(
                        aiSearchRateLimitFilter,
                        UsernamePasswordAuthenticationFilter.class
                );


        return http.build();
    }

    /**
     * 인증/인가 실패 응답을 공통 ErrorResponse 형식의 JSON으로 작성한다.
     *
     * @param response HTTP 응답
     * @param status   응답 상태 코드
     * @param code     에러 코드
     * @param message  에러 메시지
     */
    private void writeErrorResponse(
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(
                response.getWriter(),
                new ErrorResponse(code, message)
        );
    }

    /**
     * 카카오 Authorization Code를 Access Token으로 교환할 때 사용할 클라이언트.
     * 상태 코드 기반 자동 재시도가 없는 JDK HttpClient를 사용한다.
     *
     * RestClientAuthorizationCodeTokenResponseClient가 기본으로 구성하는
     * OAuth2AccessTokenResponseHttpMessageConverter(응답 파싱)와
     * OAuth2ErrorResponseErrorHandler(에러 응답 처리)를 동일하게 등록해야 한다.
     * 이 둘이 빠지면 응답 파싱이 실패해 accessToken이 null인 채로 넘어간다.
     */
    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
            kakaoAccessTokenResponseClient() {

        RestClientAuthorizationCodeTokenResponseClient client =
                new RestClientAuthorizationCodeTokenResponseClient();

        client.setRestClient(
                RestClient.builder()
                        .requestFactory(new JdkClientHttpRequestFactory())
                        .configureMessageConverters(converters -> {
                            converters.addCustomConverter(new FormHttpMessageConverter());
                            converters.addCustomConverter(new OAuth2AccessTokenResponseHttpMessageConverter());
                        })
                        .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
                        .build()
        );

        return client;
    }
}
