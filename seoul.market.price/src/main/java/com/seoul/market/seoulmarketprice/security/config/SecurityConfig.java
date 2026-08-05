package com.seoul.market.seoulmarketprice.security.config;

import com.seoul.market.seoulmarketprice.security.jwt.JwtAuthenticationFilter;
import com.seoul.market.seoulmarketprice.security.oauth2.CustomOAuth2UserService;
import com.seoul.market.seoulmarketprice.security.oauth2.OAuth2SuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

/**
 * Spring Security 설정.
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UrlBasedCorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationFilter jwtAuthenticationFilter,
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

                // 서버에 인증 세션을 저장하지 않는다.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                // Spring 기본 로그인 방식을 사용하지 않는다.
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

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
                                "/api/members/check-user-id",

                                // 회원가입 화면(로그인 전)에서 호출하는
                                // 휴대폰 PASS 본인인증 결과 확인 API
                                "/api/members/phone-verification/**",

                                "/oauth2/**",
                                "/login/oauth2/**",

                                // Swagger UI 및 OpenAPI 명세 조회는 인증 없이 접근 허용
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll();

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

                    auth.requestMatchers(HttpMethod.GET, "/api/boards", "/api/boards/**")
                            .permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/boards", "/api/boards/**")
                            .hasRole("USER");
                    auth.requestMatchers(HttpMethod.PATCH, "/api/boards/**")
                            .hasRole("USER");
                    auth.requestMatchers(HttpMethod.DELETE, "/api/boards/**")
                            .hasRole("USER");

                    // 운영 환경의 관리자 생성 및 관리자 전용 API는 ADMIN 권한이 필요하다.
                    auth.requestMatchers(
                                    "/api/admin/**",
                                    "/api/admins",
                                    "/api/admins/**"
                            )
                            .hasRole("ADMIN")
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
                );

        return http.build();
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
