package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.config.PasswordResetProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

/** DB 저장 없이 사용할 수 있는 단기 비밀번호 재설정 JWT를 관리한다. */
@Component
public class PasswordResetTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String PASSWORD_FINGERPRINT_CLAIM = "passwordFingerprint";
    private static final String PASSWORD_RESET_TYPE = "PASSWORD_RESET";

    private final PasswordResetProperties properties;

    public PasswordResetTokenProvider(PasswordResetProperties properties) {
        this.properties = properties;
    }

    public String create(Member member) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(member.getId()))
                .id(UUID.randomUUID().toString())
                .claim(TOKEN_TYPE_CLAIM, PASSWORD_RESET_TYPE)
                .claim(
                        PASSWORD_FINGERPRINT_CLAIM,
                        fingerprint(member.getPassword())
                )
                .issuedAt(Date.from(now))
                .expiration(Date.from(
                        now.plusMillis(properties.expirationMillis())
                ))
                .signWith(secretKey())
                .compact();
    }

    public PasswordResetTokenClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!PASSWORD_RESET_TYPE.equals(
                    claims.get(TOKEN_TYPE_CLAIM, String.class)
            )) {
                throw invalidToken();
            }

            String fingerprint = claims.get(
                    PASSWORD_FINGERPRINT_CLAIM,
                    String.class
            );
            if (fingerprint == null || fingerprint.isBlank()) {
                throw invalidToken();
            }

            return new PasswordResetTokenClaims(
                    Long.valueOf(claims.getSubject()),
                    fingerprint
            );
        } catch (JwtException | NumberFormatException exception) {
            throw invalidToken();
        }
    }

    public boolean matchesCurrentPassword(
            String tokenFingerprint,
            String encodedPassword
    ) {
        return MessageDigest.isEqual(
                tokenFingerprint.getBytes(StandardCharsets.UTF_8),
                fingerprint(encodedPassword).getBytes(StandardCharsets.UTF_8)
        );
    }

    public long expiresInSeconds() {
        return properties.expirationMillis() / 1000;
    }

    private String fingerprint(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalStateException(
                    "비밀번호가 없는 회원은 재설정할 수 없습니다."
            );
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    secretBytes(),
                    "HmacSHA256"
            ));
            return HexFormat.of().formatHex(
                    mac.doFinal(encodedPassword.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "비밀번호 재설정 지문을 생성할 수 없습니다.",
                    exception
            );
        }
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(secretBytes());
    }

    private byte[] secretBytes() {
        String secret = properties.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "PASSWORD_RESET_SECRET 환경변수가 설정되지 않았습니다."
            );
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "비밀번호 재설정 비밀키는 최소 32Byte 이상이어야 합니다."
            );
        }
        return bytes;
    }

    private IllegalArgumentException invalidToken() {
        return new IllegalArgumentException(
                "비밀번호 재설정 인증이 만료되었거나 유효하지 않습니다."
        );
    }

    public record PasswordResetTokenClaims(
            Long memberId,
            String passwordFingerprint
    ) {
    }
}
