package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.config.PasswordResetProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetTokenProviderTest {

    private PasswordResetTokenProvider provider;
    private Member member;

    @BeforeEach
    void setUp() {
        provider = new PasswordResetTokenProvider(
                new PasswordResetProperties(
                        "password-reset-test-secret-key-at-least-32-bytes",
                        300_000
                )
        );
        member = Member.createLocalMember(
                "seouluser01", "encoded-old-password", "홍길동",
                null, null, null, "010-1234-5678", null,
                (byte) 1, (byte) 1, (byte) 1, null
        );
        ReflectionTestUtils.setField(member, "id", 1L);
    }

    @Test
    void issuedTokenIsBoundToMemberAndCurrentPassword() {
        String token = provider.create(member);

        PasswordResetTokenProvider.PasswordResetTokenClaims claims =
                provider.parse(token);

        assertThat(claims.memberId()).isEqualTo(1L);
        assertThat(provider.matchesCurrentPassword(
                claims.passwordFingerprint(),
                member.getPassword()
        )).isTrue();

        member.changePassword("encoded-new-password");

        assertThat(provider.matchesCurrentPassword(
                claims.passwordFingerprint(),
                member.getPassword()
        )).isFalse();
    }
}
