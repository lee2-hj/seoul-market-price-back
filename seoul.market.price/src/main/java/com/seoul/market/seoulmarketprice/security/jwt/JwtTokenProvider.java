package com.seoul.market.seoulmarketprice.security.jwt;

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
     * 생성자 주입을 통해 JwtProperties를 전달받는다.
     *
     * @param jwtProperties application.yml의 JWT 설정값
     */
    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Access Token을 생성한다.
     *
     * <p>
     * subject에는 회원의 PK를 저장하고,
     * 별도의 claim에는 로그인 아이디를 저장한다.
     * </p>
     *
     * @param memberId 회원 고유번호
     * @param userId   로그인 아이디
     * @return 생성된 Access Token
     */
    public String createAccessToken(
            Long memberId,
            String userId
    ) {
        Instant now = Instant.now();

        Instant expiration = now.plusMillis(
                jwtProperties.accessTokenExpiry()
        );

        return Jwts.builder()

                /*
                 * JWT의 subject에 회원 PK를 저장한다.
                 */
                .subject(String.valueOf(memberId))

                /*
                 * JWT의 사용자 정의 Claim에
                 * 로그인 아이디를 저장한다.
                 */
                .claim("userId", userId)

                /*
                 * 토큰이 발급된 시간을 저장한다.
                 */
                .issuedAt(Date.from(now))

                /*
                 * 토큰의 만료 시간을 저장한다.
                 */
                .expiration(Date.from(expiration))

                /*
                 * 비밀키를 이용해 JWT에 서명한다.
                 */
                .signWith(getSecretKey())

                /*
                 * 설정한 내용을 최종 JWT 문자열로 생성한다.
                 */
                .compact();
    }

    /**
     * Refresh Token을 생성한다.
     *
     * <p>
     * Refresh Token은 Access Token보다 긴 유효시간을 가진다.
     * 현재는 회원 PK만 subject에 저장한다.
     * </p>
     *
     * @param memberId 회원 고유번호
     * @return 생성된 Refresh Token
     */
    public String createRefreshToken(Long memberId) {
        Instant now = Instant.now();

        Instant expiration = now.plusMillis(
                jwtProperties.refreshTokenExpiry()
        );

        return Jwts.builder()

                /*
                 * Refresh Token의 subject에 회원 PK를 저장한다.
                 */
                .subject(String.valueOf(memberId))

                /*
                 * Refresh Token 발급 시간을 저장한다.
                 */
                .issuedAt(Date.from(now))

                /*
                 * Refresh Token 만료 시간을 저장한다.
                 */
                .expiration(Date.from(expiration))

                /*
                 * Access Token과 동일한 비밀키로 서명한다.
                 */
                .signWith(getSecretKey())

                /*
                 * 최종 Refresh Token 문자열을 생성한다.
                 */
                .compact();
    }

    /**
     * JWT의 서명과 만료 여부를 검증한다.
     *
     * @param token 검증할 JWT 문자열
     * @return 정상적인 토큰이면 true, 잘못된 토큰이면 false
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;

        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * JWT에서 회원 고유번호를 조회한다.
     *
     * <p>
     * JWT subject에 저장된 문자열을 Long 타입으로 변환한다.
     * </p>
     *
     * @param token JWT 문자열
     * @return 회원 고유번호
     */
    public Long getMemberId(String token) {
        String subject = parseToken(token)
                .getPayload()
                .getSubject();

        return Long.valueOf(subject);
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
     * JWT를 해석하고 서명을 검증한다.
     *
     * <p>
     * 토큰 생성 시 사용한 것과 동일한 비밀키로 검증한다.
     * 서명이 올바르지 않거나 토큰이 만료되면 예외가 발생한다.
     * </p>
     *
     * @param token 검증할 JWT 문자열
     * @return 검증이 완료된 JWT Claims
     */
    private Jws<Claims> parseToken(String token) {
        return Jwts.parser()

                /*
                 * 토큰 생성에 사용한 비밀키로
                 * JWT 서명을 검증한다.
                 */
                .verifyWith(getSecretKey())

                /*
                 * JWT Parser를 생성한다.
                 */
                .build()

                /*
                 * 서명된 JWT를 해석한다.
                 */
                .parseSignedClaims(token);
    }

    /**
     * 일반 문자열로 작성된 JWT 비밀키를
     * SecretKey 객체로 변환한다.
     *
     * <p>
     * application.yml 또는 환경변수에서 가져온 문자열을
     * UTF-8 바이트 배열로 변환한 뒤 HMAC 비밀키를 생성한다.
     * </p>
     *
     * <p>
     * HS256 알고리즘을 안전하게 사용하려면
     * 비밀키는 최소 32바이트 이상이어야 한다.
     * </p>
     *
     * @return JWT 서명과 검증에 사용할 SecretKey
     */
    private SecretKey getSecretKey() {
        String secret = jwtProperties.secret();

        /*
         * 비밀키가 설정되지 않은 경우
         * JWT를 생성하거나 검증할 수 없으므로 예외를 발생시킨다.
         */
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET 환경변수가 설정되지 않았습니다."
            );
        }

        /*
         * 일반 문자열을 UTF-8 바이트 배열로 변환한다.
         */
        byte[] keyBytes = secret.getBytes(
                StandardCharsets.UTF_8
        );

        /*
         * HS256 사용을 위해 최소 32바이트 이상의 키인지 확인한다.
         */
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT 비밀키는 최소 32바이트 이상이어야 합니다."
            );
        }

        /*
         * 문자열 바이트 배열을 JWT 서명용 SecretKey로 변환한다.
         */
        return Keys.hmacShaKeyFor(keyBytes);
    }
}