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

/**
 * JWT 생성과 검증을 담당하는 클래스이다.
 *
 * <p>
 * Access Token과 Refresh Token을 생성하고,
 * 전달받은 토큰의 서명과 만료 여부를 검증한다.
 * </p>
 *
 * <p>
 * JWT 관련 설정값은 JwtProperties에서 전달받는다.
 * </p>
 */
@Component
public class JwtTokenProvider {

    /**
     * JWT 설정값.
     *
     * 비밀키와 Access Token, Refresh Token의 만료 시간을 가지고 있다.
     */
    private final JwtProperties jwtProperties;

    /**
     * 생성자 주입.
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
     * 토큰에는 사용자(또는 관리자) 번호,
     * 로그인 아이디,
     * 권한(USER / ADMIN)을 저장한다.
     * </p>
     *
     * @param memberId 사용자 또는 관리자 PK
     * @param userId 로그인 아이디
     * @param role 사용자 권한
     * @return Access Token
     */
    public String createAccessToken(
            Long memberId,
            String userId,
            Role role
    ) {

        Instant now = Instant.now();

        Instant expiration =
                now.plusMillis(
                        jwtProperties.accessTokenExpiry()
                );

        return Jwts.builder()

                // 사용자 PK
                .subject(String.valueOf(memberId))

                // 로그인 아이디
                .claim("userId", userId)

                // USER / ADMIN
                .claim("role", role.name())

                // 발급시간
                .issuedAt(Date.from(now))

                // 만료시간
                .expiration(Date.from(expiration))

                // 서명
                .signWith(getSecretKey())

                .compact();
    }

    /**
     * Refresh Token을 생성한다.
     *
     * @param memberId 회원 번호
     * @return Refresh Token
     */
    public String createRefreshToken(Long memberId) {

        Instant now = Instant.now();

        Instant expiration =
                now.plusMillis(
                        jwtProperties.refreshTokenExpiry()
                );

        return Jwts.builder()

                .subject(String.valueOf(memberId))

                .issuedAt(Date.from(now))

                .expiration(Date.from(expiration))

                .signWith(getSecretKey())

                .compact();
    }

    /**
     * JWT가 정상인지 검증한다.
     *
     * @param token JWT
     * @return 정상 여부
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
     * JWT에서 회원 번호를 조회한다.
     *
     * @param token JWT
     * @return 회원 번호
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
     * @param token Access Token
     * @return USER 또는 ADMIN
     */
    public Role getRole(String token) {

        String role = parseToken(token)
                .getPayload()
                .get("role", String.class);

        return Role.valueOf(role);
    }

    /**
     * JWT를 해석하고 서명을 검증한다.
     *
     * @param token JWT
     * @return Claims
     */
    private Jws<Claims> parseToken(String token) {

        return Jwts.parser()

                .verifyWith(getSecretKey())

                .build()

                .parseSignedClaims(token);
    }

    /**
     * 문자열 비밀키를 SecretKey로 변환한다.
     *
     * @return SecretKey
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