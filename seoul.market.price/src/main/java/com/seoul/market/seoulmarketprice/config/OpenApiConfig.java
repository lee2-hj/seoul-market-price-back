package com.seoul.market.seoulmarketprice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(springdoc-openapi) 설정.
 * /swagger-ui.html 에서 API 명세를 확인할 수 있다.
 */
@Configuration
public class OpenApiConfig {

    private static final String JWT_SCHEME_NAME = "JWT";

    @Bean
    public OpenAPI openAPI() {

        // Swagger UI 우측 상단 "Authorize" 버튼으로 JWT Access Token을 입력할 수 있게 하는 인증 스킴
        SecurityScheme jwtScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        // 모든 API에 위 인증 스킴을 기본으로 적용
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(JWT_SCHEME_NAME);

        // API 문서 제목/설명 등 기본 정보
        Info info = new Info()
                .title("Seoul Market Price API")
                .description("서울 시장 가격 정보 서비스 API 명세서")
                .version("v1");

        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(new Components().addSecuritySchemes(JWT_SCHEME_NAME, jwtScheme));
    }
}
