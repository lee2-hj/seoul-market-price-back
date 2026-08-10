package com.seoul.market.seoulmarketprice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 비밀번호 재설정 전용 JWT 설정이다. */
@ConfigurationProperties(prefix = "app.password-reset")
public record PasswordResetProperties(
        String secret,
        long expirationMillis
) {
}
