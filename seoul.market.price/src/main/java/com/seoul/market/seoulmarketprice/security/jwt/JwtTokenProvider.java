package com.seoul.market.seoulmarketprice.security.jwt;

import com.seoul.market.seoulmarketprice.auth.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 생성과 검증을 담당하는 클래스이다.
 *
 * <p>
 * Access Token과 Refresh Token을 생성하고,
 * 전달받은 토큰의 서명과 만료 여부를 검증한다.
 * </p>
 *
 * <p>
 * 토큰에는 권한과 토큰 종류를 저장하여
 * 일반 회원과 관리자를 구분하고,
 * Access Token과 Refresh Token이 서로 다른 용도로
 * 사용되도록 한다.
 * </p>
 */
@Component
public class JwtTokenProvider {

    /**
     * JWT Claim에 저장할 토큰 종류의 Key이다.
     */
    private static final String TOKEN_TYPE_CLAIM = "tokenType";

    /**
     * JWT Claim에 저장할 권한의 Key이다.
     */
    private static final String ROLE_CLAIM = "role";

    /**
     * Access Token을 나타내는 값이다.
     */
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";

    /**
     * Refresh Token을 나타내는 값이다.
     */
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    /**
     * JWT 설정값이다.
     */
    private final JwtProperties jwtProperties;

    /**
     * 생성자 주입을 사용한다.
     *
     * @param jwtProperties JWT 설정값
     */
    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Access Token을 생성한다.
     *
     * <p>
     * 사용자 또는 관리자 PK, 로그인 아이디,
     * 권한과 토큰 종류를 저장한다.
     * </p>
     *
     * @param principalId 사용자 또는 관리자 PK
     * @param userId      로그인 아이디
     * @param role        USER 또는 ADMIN
     * @return 생성된 Access Token
     */
    public String createAccessToken(
            Long principalId,
            String userId,
            Role role
    ) {
        Instant now = Instant.now();

        Instant expiration =
                now.plusMillis(
                        jwtProperties.accessTokenExpiry()
                );

        return Jwts.builder()

                // 일반 회원 또는 관리자 PK
                .subject(String.valueOf(principalId))

                /*
                 * 토큰마다 다른 고유 식별자를 생성한다.
                 * 같은 관리자가 같은 시각에 토큰을 발급받더라도
                 * 서로 다른 JWT가 생성되도록 고유 식별자를 저장한다.
                 */
                .id(UUID.randomUUID().toString())

                // 로그인 아이디
                .claim("userId", userId)

                // USER 또는 ADMIN
                .claim(ROLE_CLAIM, role.name())

                // ACCESS Token임을 표시한다.
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)

                // 발급 시각
                .issuedAt(Date.from(now))

                // 만료 시각
                .expiration(Date.from(expiration))

                // 비밀키로 서명
                .signWith(getSecretKey())

                .compact();
    }

    /**
     * 일반 회원용 Refresh Token을 생성한다.
     *
     * <p>
     * 기존 일반 회원 인증 코드와의 호환성을 유지하기 위한 메서드이다.
     * 내부적으로 USER 권한이 포함된 Refresh Token을 생성한다.
     * </p>
     *
     * @param memberId 일반 회원 PK
     * @return 일반 회원 Refresh Token
     */
    public String createRefreshToken(Long memberId) {
        return createRefreshToken(
                memberId,
                Role.USER
        );
    }

    /**
     * 권한이 포함된 Refresh Token을 생성한다.
     *
     * <p>
     * 일반 회원과 관리자의 Refresh Token을 구분할 수 있도록
     * USER 또는 ADMIN 권한을 Claim에 저장한다.
     * </p>
     *
     * @param principalId 일반 회원 또는 관리자 PK
     * @param role        USER 또는 ADMIN
     * @return 생성된 Refresh Token
     */
    public String createRefreshToken(
            Long principalId,
            Role role
    ) {
        Instant now = Instant.now();

        Instant expiration =
                now.plusMillis(
                        jwtProperties.refreshTokenExpiry()
                );

        return Jwts.builder()

                // 일반 회원 또는 관리자 PK
                .subject(String.valueOf(principalId))

                /*
                 *  토큰마다 다른 고유 식별자를 생성한다.
                 * 같은 사용자가 같은 시각에 토큰을 발급받더라도
                 * 서로 다른 Refresh Token이 생성되도록 한다.
                 */
                .id(UUID.randomUUID().toString())

                // USER 또는 ADMIN
                .claim(ROLE_CLAIM, role.name())

                // REFRESH Token임을 표시한다.
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)

                // 발급 시각
                .issuedAt(Date.from(now))

                // 만료 시각
                .expiration(Date.from(expiration))

                // 비밀키로 서명
                .signWith(getSecretKey())

                .compact();
    }

    /**
     * JWT의 서명과 만료 여부를 검증한다.
     *
     * @param token 검증할 JWT
     * @return 정상적인 JWT이면 {@code true}
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 전달받은 JWT가 Access Token인지 확인한다.
     *
     * @param token 검사할 JWT
     * @return Access Token이면 {@code true}
     */
    public boolean isAccessToken(String token) {
        return ACCESS_TOKEN_TYPE.equals(
                getTokenType(token)
        );
    }

    /**
     * 전달받은 JWT가 Refresh Token인지 확인한다.
     *
     * @param token 검사할 JWT
     * @return Refresh Token이면 {@code true}
     */
    public boolean isRefreshToken(String token) {
        return REFRESH_TOKEN_TYPE.equals(
                getTokenType(token)
        );
    }

    /**
     * JWT에서 일반 회원 또는 관리자 PK를 조회한다.
     *
     * @param token JWT
     * @return 일반 회원 또는 관리자 PK
     */
    public Long getMemberId(String token) {
        return Long.valueOf(
                parseToken(token)
                        .getPayload()
                        .getSubject()
        );
    }

    /**
     * JWT에서 로그인 아이디를 조회한다.
     *
     * @param token Access Token
     * @return 로그인 아이디
     */
    public String getUserId(String token) {
        return parseToken(token)
                .getPayload()
                .get("userId", String.class);
    }

    /**
     * JWT에서 사용자 권한을 조회한다.
     *
     * @param token JWT
     * @return USER 또는 ADMIN
     */
    public Role getRole(String token) {
        String role = parseToken(token)
                .getPayload()
                .get(ROLE_CLAIM, String.class);

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT에 권한 정보가 존재하지 않습니다."
            );
        }

        return Role.valueOf(role);
    }

    /**
     * JWT에서 토큰 종류를 조회한다.
     *
     * @param token JWT
     * @return ACCESS 또는 REFRESH
     */
    private String getTokenType(String token) {
        return parseToken(token)
                .getPayload()
                .get(TOKEN_TYPE_CLAIM, String.class);
    }

    /**
     * JWT를 해석하고 서명을 검증한다.
     *
     * @param token JWT
     * @return JWT의 Claims
     */
    private Jws<Claims> parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token);
    }

    /**
     * 문자열 비밀키를 JWT 서명용 SecretKey로 변환한다.
     *
     * @return JWT 서명용 비밀키
     */
    private SecretKey getSecretKey() {
        String secret = jwtProperties.secret();

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET 환경변수가 설정되지 않았습니다."
            );
        }

        byte[] keyBytes =
                secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT 비밀키는 최소 32Byte 이상이어야 합니다."
            );
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}