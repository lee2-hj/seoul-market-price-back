package com.seoul.market.seoulmarketprice.member.dto.response.member;

/** 본인 확인 후 발급한 단기 비밀번호 재설정 토큰이다. */
public record PasswordResetVerifyResponse(
        boolean verified,
        String resetToken,
        long expiresInSeconds
) {
}
