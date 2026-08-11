package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.config.PasswordResetProperties;
import com.seoul.market.seoulmarketprice.member.dto.request.member.PasswordResetCompleteRequest;
import com.seoul.market.seoulmarketprice.member.dto.request.member.PasswordResetVerifyRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.member.PasswordResetVerifyResponse;
import com.seoul.market.seoulmarketprice.member.repository.MemberManagementRepository;
import com.seoul.market.seoulmarketprice.phoneverification.dto.request.PhoneVerificationConfirmRequest;
import com.seoul.market.seoulmarketprice.phoneverification.dto.response.PhoneVerificationConfirmResponse;
import com.seoul.market.seoulmarketprice.phoneverification.service.PhoneVerificationService;
import com.seoul.market.seoulmarketprice.token.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock PhoneVerificationService phoneVerificationService;
    @Mock MemberManagementRepository memberManagementRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RefreshTokenService refreshTokenService;

    private PasswordResetTokenProvider tokenProvider;
    private PasswordResetService service;
    private Member member;

    @BeforeEach
    void setUp() {
        tokenProvider = new PasswordResetTokenProvider(
                new PasswordResetProperties(
                        "password-reset-test-secret-key-at-least-32-bytes",
                        300_000
                )
        );
        service = new PasswordResetService(
                phoneVerificationService,
                memberManagementRepository,
                tokenProvider,
                passwordEncoder,
                refreshTokenService
        );
        member = Member.createLocalMember(
                "seouluser01", "encoded-old-password", "홍길동",
                null, null, null, "010-1234-5678", null,
                (byte) 1, (byte) 1, (byte) 1, null
        );
        ReflectionTestUtils.setField(member, "id", 1L);
    }

    @Test
    void verifiesPassIdentityAndChangesPasswordOnlyOnce() {
        when(phoneVerificationService.confirm(
                new PhoneVerificationConfirmRequest("verification-id")
        )).thenReturn(new PhoneVerificationConfirmResponse(
                true, "홍길동", "+82 10-1234-5678", null, null,
                Instant.now().toString(), "ci-value"
        ));
        when(memberManagementRepository.findActiveLocalByUserIdForCiRegistration(
                "seouluser01"
        )).thenReturn(Optional.of(member));
        when(memberManagementRepository.existsByCi("ci-value")).thenReturn(false);

        PasswordResetVerifyResponse verified = service.verify(
                new PasswordResetVerifyRequest(
                        "verification-id",
                        "seouluser01"
                )
        );

        when(memberManagementRepository.findActiveByIdForPasswordReset(1L))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches("new-password", "encoded-old-password"))
                .thenReturn(false);
        when(passwordEncoder.encode("new-password"))
                .thenReturn("encoded-new-password");

        service.complete(new PasswordResetCompleteRequest(
                verified.resetToken(),
                "new-password",
                "new-password"
        ));

        assertThat(member.getPassword()).isEqualTo("encoded-new-password");
        assertThat(member.getCi()).isEqualTo("ci-value");
        verify(refreshTokenService).deleteAllByMemberId(1L);
    }
}
