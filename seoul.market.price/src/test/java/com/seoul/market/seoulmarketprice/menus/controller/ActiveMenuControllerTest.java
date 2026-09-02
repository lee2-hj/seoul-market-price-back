package com.seoul.market.seoulmarketprice.menus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoul.market.seoulmarketprice.ai.filter.AiSearchRateLimitFilter;
import com.seoul.market.seoulmarketprice.config.FrontendProperties;
import com.seoul.market.seoulmarketprice.menus.service.ActiveMenuService;
import com.seoul.market.seoulmarketprice.security.config.CorsConfig;
import com.seoul.market.seoulmarketprice.security.config.SecurityConfig;
import com.seoul.market.seoulmarketprice.security.jwt.JwtAuthenticationFilter;
import com.seoul.market.seoulmarketprice.security.oauth2.CustomOAuth2UserService;
import com.seoul.market.seoulmarketprice.security.oauth2.OAuth2SuccessHandler;
import com.seoul.market.seoulmarketprice.security.principal.CustomUserPrincipal;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 {@link SecurityConfig} 인가 규칙을 적용한 상태로
 * {@code /api/activeMenu/me}, {@code /api/activeMenu/{id}}의
 * 인증/인가 결과(401/403/200)를 검증한다.
 */
@WebMvcTest(controllers = ActiveMenuController.class)
@ContextConfiguration(classes = ActiveMenuControllerTest.TestApplication.class)
@Import({SecurityConfig.class, CorsConfig.class, ActiveMenuControllerTest.TestConfig.class})
class ActiveMenuControllerTest {

    /**
     * 실제 {@code Application}(⟨@EnableJpaAuditing⟩ 포함)을 루트 설정으로 사용하면
     * 이 슬라이스 테스트에는 없는 JPA 메타모델을 요구하며 컨텍스트 로딩이 실패한다.
     * JPA 감사 기능과 무관한 순수 MVC/Security 슬라이스이므로 별도의 최소 루트 설정을 사용한다.
     */
    @SpringBootConfiguration
    @ComponentScan(
            basePackageClasses = ActiveMenuController.class,
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ActiveMenuController.class)
    )
    static class TestApplication {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActiveMenuService activeMenuService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private AiSearchRateLimitFilter aiSearchRateLimitFilter;
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private OAuth2SuccessHandler oauth2SuccessHandler;

    @TestConfiguration
    static class TestConfig {
        @Bean
        FrontendProperties frontendProperties() {
            return new FrontendProperties("http://localhost:5173/login", "localhost:5173");
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        /** oauth2Login() 설정이 요구하는 빈으로, 이 테스트에서는 실제로 사용되지 않는다. */
        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return mock(ClientRegistrationRepository.class);
        }
    }

    /** JWT/속도제한 필터는 이 테스트의 관심사가 아니므로 다음 필터로 그대로 넘긴다. */
    @BeforeEach
    void passThroughUnrelatedFilters() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(aiSearchRateLimitFilter).doFilter(any(), any(), any());
    }

    private Authentication authenticationOf(Long memberId, String userId, String role) {
        CustomUserPrincipal principal = new CustomUserPrincipal(memberId, userId);
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(role))
        );
    }

    @Test
    void meWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/api/activeMenu/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithUserRoleReturns403() throws Exception {
        mockMvc.perform(get("/api/activeMenu/me")
                        .with(authentication(authenticationOf(1L, "user01", "ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void meWithAdminRoleReturns200() throws Exception {
        when(activeMenuService.getActiveMenu(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/activeMenu/me")
                        .with(authentication(authenticationOf(1L, "admin01", "ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void meWithMasterRoleReturns200() throws Exception {
        when(activeMenuService.getActiveMenu(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/activeMenu/me")
                        .with(authentication(authenticationOf(1L, "master01", "ROLE_MASTER"))))
                .andExpect(status().isOk());
    }

    @Test
    void idPathWithAdminRoleReturns403() throws Exception {
        mockMvc.perform(get("/api/activeMenu/2")
                        .with(authentication(authenticationOf(1L, "admin01", "ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void idPathWithUserRoleReturns403() throws Exception {
        mockMvc.perform(get("/api/activeMenu/2")
                        .with(authentication(authenticationOf(1L, "user01", "ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void idPathWithMasterRoleReturns200() throws Exception {
        when(activeMenuService.getActiveMenu(eq(2L), eq(1L), eq(true))).thenReturn(List.of());

        mockMvc.perform(get("/api/activeMenu/2")
                        .with(authentication(authenticationOf(1L, "master01", "ROLE_MASTER"))))
                .andExpect(status().isOk());
    }
}
