package com.seoul.market.seoulmarketprice.security.jwt;

import com.seoul.market.seoulmarketprice.auth.entity.Role;
import com.seoul.market.seoulmarketprice.security.principal.CustomUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * HTTP 요청의 Access Token을 검사하는 JWT 인증 필터이다.
 *
 * <p>
 * 요청의 Authorization 헤더에서 Bearer Token을 꺼내고,
 * 토큰이 정상이라면 로그인 사용자 정보와 권한을
 * Spring Security에 등록한다.
 * </p>
 *
 * <p>
 * 이 필터는 로그인 자체를 처리하지 않는다.
 * 로그인 과정에서 발급된 Access Token을 검증하고,
 * 인증된 사용자 정보를 현재 요청에 등록하는 역할을 한다.
 * </p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * JWT 생성, 검증 및 정보 추출을 담당한다.
     */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 생성자 주입을 사용한다.
     *
     * @param jwtTokenProvider JWT 처리 클래스
     */
    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 요청마다 JWT 인증을 처리한다.
     *
     * <ol>
     *     <li>Authorization 헤더에서 Access Token을 추출한다.</li>
     *     <li>토큰의 서명과 만료 여부를 검사한다.</li>
     *     <li>토큰에서 고유번호, 로그인 아이디, 권한을 꺼낸다.</li>
     *     <li>CustomUserPrincipal을 생성한다.</li>
     *     <li>권한을 포함한 Authentication을 SecurityContext에 저장한다.</li>
     * </ol>
     *
     * @param request 현재 HTTP 요청
     * @param response 현재 HTTP 응답
     * @param filterChain 다음 필터로 요청을 전달하는 객체
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Authorization 헤더에서 Access Token을 추출한다.
        String accessToken = resolveAccessToken(request);

        /*
         * 토큰이 존재하고 유효하며,
         * 현재 요청에 인증 정보가 아직 없는 경우에만
         * 새로운 인증 객체를 생성한다.
         */
        if (
                accessToken != null
                        && jwtTokenProvider.validateToken(accessToken)
                        && SecurityContextHolder.getContext()
                        .getAuthentication() == null
        ) {
            // JWT에서 사용자 또는 관리자 고유번호를 꺼낸다.
            Long memberId =
                    jwtTokenProvider.getMemberId(accessToken);

            // JWT에서 로그인 아이디를 꺼낸다.
            String userId =
                    jwtTokenProvider.getUserId(accessToken);

            // JWT에서 USER 또는 ADMIN 권한을 꺼낸다.
            Role role =
                    jwtTokenProvider.getRole(accessToken);

            /*
             * Member 엔티티를 직접 저장하지 않고,
             * 인증에 필요한 최소 정보만 Principal에 담는다.
             */
            CustomUserPrincipal principal =
                    new CustomUserPrincipal(
                            memberId,
                            userId
                    );

            /*
             * Spring Security의 hasRole("ADMIN")은
             * 내부적으로 ROLE_ADMIN 권한을 확인한다.
             *
             * USER  → ROLE_USER
             * ADMIN → ROLE_ADMIN
             */
            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority(
                            "ROLE_" + role.name()
                    );

            // 사용자 정보와 권한을 담은 인증 객체를 생성한다.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(authority)
                    );

            /*
             * 현재 요청의 인증 정보를 SecurityContext에 저장한다.
             *
             * 이후 Controller의 @AuthenticationPrincipal과
             * SecurityConfig의 hasRole()에서 사용할 수 있다.
             */
            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);
        }

        /*
         * 토큰이 없거나 잘못된 경우에도 다음 필터로 요청을 넘긴다.
         * 보호된 API라면 Spring Security가 최종적으로 접근을 거부한다.
         */
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Access Token을 추출한다.
     *
     * <p>
     * 정상적인 헤더 형식은 다음과 같다.
     * </p>
     *
     * <pre>
     * Authorization: Bearer eyJhbGciOi...
     * </pre>
     *
     * @param request 현재 HTTP 요청
     * @return Bearer 접두사를 제거한 JWT 문자열 또는 null
     */
    private String resolveAccessToken(
            HttpServletRequest request
    ) {
        String authorizationHeader =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        // Authorization 헤더가 없거나 비어 있으면 null을 반환한다.
        if (
                authorizationHeader == null
                        || authorizationHeader.isBlank()
        ) {
            return null;
        }

        String bearerPrefix = "Bearer ";

        // Bearer 형식이 아니면 JWT 요청으로 처리하지 않는다.
        if (!authorizationHeader.startsWith(bearerPrefix)) {
            return null;
        }

        // "Bearer " 뒤의 실제 JWT 문자열만 추출한다.
        String accessToken =
                authorizationHeader.substring(
                        bearerPrefix.length()
                ).trim();

        // Bearer 뒤에 토큰 값이 없으면 null을 반환한다.
        if (accessToken.isBlank()) {
            return null;
        }

        return accessToken;
    }
}