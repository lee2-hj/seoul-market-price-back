package com.seoul.market.seoulmarketprice.security.jwt;

import com.seoul.market.seoulmarketprice.auth.entity.Role;
import com.seoul.market.seoulmarketprice.auth.repository.AdminRepository;
import com.seoul.market.seoulmarketprice.auth.repository.MemberRepository;
import com.seoul.market.seoulmarketprice.security.principal.CustomUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
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
 * Authorization 헤더에서 Bearer Token을 추출하고,
 * 유효한 Access Token인 경우 인증 정보를
 * Spring Security에 등록한다.
 * </p>
 *
 * <p>
 * Refresh Token은 토큰 재발급에만 사용하며,
 * 이 필터를 통한 사용자 인증에는 사용할 수 없다.
 * </p>
 */
@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    /**
     * JWT 생성, 검증 및 정보 추출을 담당한다.
     */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 삭제되지 않은 관리자 계정인지 확인한다.
     */
    private final AdminRepository adminRepository;

    /** 삭제된 일반 회원의 기존 Access Token을 즉시 차단하는 활성 상태 조회 저장소. */
    private final MemberRepository memberRepository;

    /**
     * 생성자 주입을 사용한다.
     *
     * @param jwtTokenProvider JWT 처리 클래스
     * @param adminRepository  관리자 조회 Repository
     */
    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            AdminRepository adminRepository,
            MemberRepository memberRepository
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.adminRepository = adminRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * 요청마다 JWT 인증을 처리한다.
     *
     * <ol>
     *     <li>Authorization 헤더에서 JWT를 추출한다.</li>
     *     <li>JWT의 서명과 만료 여부를 확인한다.</li>
     *     <li>Access Token인지 확인한다.</li>
     *     <li>PK, 로그인 아이디, 권한을 추출한다.</li>
     *     <li>관리자라면 삭제되지 않은 계정인지 확인한다.</li>
     *     <li>인증 정보를 SecurityContext에 저장한다.</li>
     * </ol>
     *
     * @param request     현재 HTTP 요청
     * @param response    현재 HTTP 응답
     * @param filterChain 다음 필터
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Authorization 헤더에서 JWT를 추출한다.
        String accessToken = resolveAccessToken(request);

        /*
         * 다음 조건을 모두 만족할 때만 인증 정보를 생성한다.
         *
         * 1. 토큰이 존재한다.
         * 2. 서명과 만료 시간이 유효하다.
         * 3. 토큰 종류가 ACCESS이다.
         * 4. 기존 인증 정보가 없다.
         */
        if (
                accessToken != null
                        && jwtTokenProvider.validateToken(accessToken)
                        && jwtTokenProvider.isAccessToken(accessToken)
        ) {
            // 일반 회원 또는 관리자 PK를 가져온다.
            Long principalId =
                    jwtTokenProvider.getMemberId(accessToken);

            // 로그인 아이디를 가져온다.
            String userId =
                    jwtTokenProvider.getUserId(accessToken);

            // USER 또는 ADMIN 권한을 가져온다.
            Role role =
                    jwtTokenProvider.getRole(accessToken);

            /*
             * 삭제된 관리자의 기존 Access Token은
             * 만료 여부와 관계없이 즉시 인증에서 제외한다.
             */
            if (
                    role == Role.ADMIN
                            && !adminRepository.existsActiveById(principalId)
            ) {
                filterChain.doFilter(request, response);
                return;
            }

            // 서명과 만료가 유효해도 탈퇴한 일반 회원의 Access Token은 인증하지 않는다.
            if (role == Role.USER
                    && !memberRepository.existsActiveById(principalId)) {
                filterChain.doFilter(request, response);
                return;
            }

            /*
             * Entity를 직접 저장하지 않고,
             * 인증에 필요한 최소 정보만 Principal에 저장한다.
             */
            CustomUserPrincipal principal =
                    new CustomUserPrincipal(
                            principalId,
                            userId,
                            role
                    );

            /*
             * hasRole("ADMIN")은 내부적으로 ROLE_ADMIN 권한을 확인한다.
             * "ROLE_" 접두사 중복을 방지하여 권한을 생성한다.
             */
            String roleName = role.name().startsWith("ROLE_") ? role.name() : "ROLE_" + role.name();
            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority(roleName);

            // Principal과 권한을 가진 인증 객체를 생성한다.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(authority)
                    );

            // 현재 요청의 인증 정보를 등록한다.
            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);
        }


        /*
         * 인증 여부와 관계없이 다음 필터로 요청을 전달한다.
         * 보호된 API의 접근 여부는 Spring Security가 결정한다.
         */
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer Token을 추출한다.
     *
     * <pre>
     * Authorization: Bearer eyJhbGciOi...
     * </pre>
     *
     * @param request 현재 HTTP 요청
     * @return Bearer 접두사를 제거한 JWT 또는 {@code null}
     */
    private String resolveAccessToken(
            HttpServletRequest request
    ) {
        String authorizationHeader =
                request.getHeader(
                        HttpHeaders.AUTHORIZATION
                );

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return findCookie(request, "adminAccessToken");
        }

        String bearerPrefix = "Bearer ";

        if (!authorizationHeader.startsWith(bearerPrefix)) {
            return null;
        }

        String accessToken =
                authorizationHeader
                        .substring(bearerPrefix.length())
                        .trim();

        if (accessToken.isBlank()) {
            return null;
        }

        return accessToken;
    }

    private String findCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
