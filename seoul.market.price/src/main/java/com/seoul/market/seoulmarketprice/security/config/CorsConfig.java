package com.seoul.market.seoulmarketprice.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * React와 Spring Boot 사이의 교차 출처 요청을 허용하는 설정 클래스이다.
 *
 * <p>
 * 현재 개발 환경에서는 React가 localhost:5173,
 * Spring Boot가 localhost:8081에서 실행되므로
 * 브라우저 기준으로 서로 다른 출처에 해당한다.
 * </p>
 *
 * <p>
 * Refresh Token을 HttpOnly 쿠키로 주고받기 위해
 * credentials 사용을 허용한다.
 * </p>
 */
@Configuration
public class CorsConfig {

    /**
     * Spring Security에서 사용할 CORS 설정을 등록한다.
     *
     * @return URL별 CORS 설정을 관리하는 객체
     */
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        /*
         * 요청을 허용할 프론트엔드 주소이다.
         *
         * 쿠키 전달을 허용하는 경우 "*"를 사용할 수 없으므로
         * React 개발 서버 주소를 정확하게 지정한다.
         */
        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:5173",
                        "http://localhost:3000"
                        
                )
        );

        /*
         * 프론트엔드에서 사용할 수 있는 HTTP 요청 방식을 지정한다.
         *
         * OPTIONS는 브라우저가 실제 요청 전에 보내는
         * Preflight 요청을 처리하기 위해 필요하다.
         */
        configuration.setAllowedMethods(
                List.of(
                        HttpMethod.GET.name(),
                        HttpMethod.POST.name(),
                        HttpMethod.PUT.name(),
                        HttpMethod.PATCH.name(),
                        HttpMethod.DELETE.name(),
                        HttpMethod.OPTIONS.name()
                )
        );

        /*
         * 프론트엔드가 요청에 포함할 수 있는 헤더를 지정한다.
         *
         * Authorization은 Access Token 전달에 사용하고,
         * Content-Type은 JSON 요청에 사용한다.
         */
        configuration.setAllowedHeaders(
                List.of(
                        HttpHeaders.AUTHORIZATION,
                        HttpHeaders.CONTENT_TYPE
                )
        );

        /*
         * 브라우저가 쿠키를 포함해 요청할 수 있도록 허용한다.
         *
         * Refresh Token을 HttpOnly 쿠키로 사용하므로 반드시 필요하다.
         */
        configuration.setAllowCredentials(true);

        /*
         * Preflight 요청 결과를 브라우저가 캐시할 시간이다.
         *
         * 단위는 초이며, 여기서는 1시간 동안 캐시한다.
         */
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        /*
         * 백엔드의 모든 요청 경로에 위 CORS 설정을 적용한다.
         */
        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}
