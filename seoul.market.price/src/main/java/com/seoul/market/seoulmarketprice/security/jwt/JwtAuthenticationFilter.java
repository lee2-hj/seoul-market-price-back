package com.seoul.market.seoulmarketprice.security.jwt;

import com.seoul.market.seoulmarketprice.security.principal.CustomUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
 * 토큰이 정상이라면 로그인 사용자 정보를 Spring Security에 등록한다.
 * </p>
 *
 * <p>
 * 이 필터는 비밀번호 로그인을 처리하지 않는다.
 * 이미 로그인 과정에서 발급된 Access Token을 검증하는 역할만 한다.
 * </p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * JWT 생성, 검증 및 사용자 정보 추출을 담당한다.
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
     *     <li>토큰에서 회원 번호와 로그인 아이디를 꺼낸다.</li>
     *     <li>CustomUserPrincipal을 생성한다.</li>
     *     <li>Authentication을 SecurityContext에 저장한다.</li>
     * </ol>
     *
     * @param request     현재 HTTP 요청
     * @param response    현재 HTTP 응답
     * @param filterChain 다음 필터로 요청을 전달하는 객체
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        /*
         * Authorization 헤더에서 Bearer Token을 추출한다.
         *
         * 헤더가 없거나 형식이 올바르지 않으면 null이 반환된다.
         */
        String accessToken = resolveAccessToken(request);

        /*
         * 토큰이 존재하고 정상적인 경우에만 인증 정보를 등록한다.
         *
         * 이미 앞선 필터에서 인증 정보가 등록되어 있다면
         * 중복으로 인증 객체를 생성하지 않는다.
         */
        if (
                accessToken != null
                        && jwtTokenProvider.validateToken(accessToken)
                        && SecurityContextHolder.getContext()
                        .getAuthentication() == null
        ) {
            /*
             * JWT에서 회원 고유번호와 로그인 아이디를 꺼낸다.
             */
            Long memberId =
                    jwtTokenProvider.getMemberId(accessToken);

            String userId =
                    jwtTokenProvider.getUserId(accessToken);

            /*
             * Member 엔티티 대신 인증에 필요한 정보만 담은
             * Principal 객체를 생성한다.
             */
            CustomUserPrincipal principal =
                    new CustomUserPrincipal(
                            memberId,
                            userId
                    );

            /*
             * 현재 프로젝트의 tb_user 정의서에는
             * 권한 컬럼이 없으므로 권한 목록은 비워 둔다.
             *
             * 나중에 USER, ADMIN 권한이 확정되면
             * 이 부분에 GrantedAuthority를 추가할 수 있다.
             */
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of()
                    );

            /*
             * 현재 요청의 인증 정보를 Spring Security에 저장한다.
             *
             * 이후 Controller에서는 @AuthenticationPrincipal을 통해
             * 현재 로그인 사용자를 조회할 수 있다.
             */
            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);
        }

        /*
         * JWT가 없거나 잘못됐더라도 여기서 바로 응답을 끝내지 않는다.
         *
         * 다음 필터로 요청을 넘기고,
         * 보호된 API인 경우 Spring Security가 최종적으로 접근을 거부한다.
         */
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Access Token을 추출한다.
     *
     * <p>
     * 정상적인 요청 헤더 형식은 다음과 같다.
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

        /*
         * Authorization 헤더가 없거나 비어 있으면
         * Access Token이 없는 요청으로 처리한다.
         */
        if (
                authorizationHeader == null
                        || authorizationHeader.isBlank()
        ) {
            return null;
        }

        String bearerPrefix = "Bearer ";

        /*
         * Authorization 헤더가 Bearer 형식이 아니면
         * JWT 인증 요청으로 처리하지 않는다.
         */
        if (!authorizationHeader.startsWith(bearerPrefix)) {
            return null;
        }

        /*
         * "Bearer " 뒤에 있는 JWT 문자열만 반환한다.
         */
        String accessToken =
                authorizationHeader.substring(
                        bearerPrefix.length()
                ).trim();

        /*
         * Bearer 뒤에 실제 토큰이 없는 경우를 방지한다.
         */
        if (accessToken.isBlank()) {
            return null;
        }

        return accessToken;
    }
}