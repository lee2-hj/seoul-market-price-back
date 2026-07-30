package com.seoul.market.seoulmarketprice.security.config;

import com.seoul.market.seoulmarketprice.security.jwt.JwtAuthenticationFilter;
import com.seoul.market.seoulmarketprice.security.oauth2.CustomOAuth2UserService;
import com.seoul.market.seoulmarketprice.security.oauth2.OAuth2SuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 설정.
 */
@Configuration
public class SecurityConfig {

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
                                "/api/members",
                                "/api/members/check-user-id",
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

                        // 카카오 사용자 조회 및 회원 저장 처리
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(
                                        customOAuth2UserService
                                )
                        )

                        // 로그인 성공 후 JWT 발급 처리
                        .successHandler(oauth2SuccessHandler)
                )

                // Access Token 인증 필터를 등록한다.
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
