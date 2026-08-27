package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.member.dto.request.member.PasswordResetCompleteRequest;
import com.seoul.market.seoulmarketprice.member.dto.request.member.PasswordResetVerifyRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.member.PasswordResetCompleteResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.member.PasswordResetVerifyResponse;
import com.seoul.market.seoulmarketprice.member.repository.MemberManagementRepository;
import com.seoul.market.seoulmarketprice.phoneverification.dto.request.PhoneVerificationConfirmRequest;
import com.seoul.market.seoulmarketprice.phoneverification.dto.response.PhoneVerificationConfirmResponse;
import com.seoul.market.seoulmarketprice.phoneverification.service.PhoneVerificationService;
import com.seoul.market.seoulmarketprice.token.service.RefreshTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;

/** PASS 본인확인을 거친 일반 회원의 비밀번호 재설정을 처리한다. */
@Service
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final String MEMBER_MISMATCH_MESSAGE =
            "입력한 정보와 일치하는 회원을 확인할 수 없습니다.";
    private static final Duration MAX_VERIFICATION_AGE = Duration.ofMinutes(5);
    private static final Duration FUTURE_CLOCK_TOLERANCE = Duration.ofMinutes(1);

    private final PhoneVerificationService phoneVerificationService;
    private final MemberManagementRepository memberManagementRepository;
    private final PasswordResetTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public PasswordResetService(
            PhoneVerificationService phoneVerificationService,
            MemberManagementRepository memberManagementRepository,
            PasswordResetTokenProvider tokenProvider,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService
    ) {
        this.phoneVerificationService = phoneVerificationService;
        this.memberManagementRepository = memberManagementRepository;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public PasswordResetVerifyResponse verify(
            PasswordResetVerifyRequest request
    ) {
        PhoneVerificationConfirmResponse verification =
                phoneVerificationService.confirm(
                        new PhoneVerificationConfirmRequest(
                                request.identityVerificationId()
                        )
                );

        validateRecentVerification(verification.verifiedAt());

        Member member = memberManagementRepository
                .findActiveLocalByUserIdForCiRegistration(request.userId())
                .orElseThrow(() -> new IllegalArgumentException(
                        MEMBER_MISMATCH_MESSAGE
                ));

        String verifiedCi = verification.ci();
        if (member.hasCi()) {
            if (!member.getCi().equals(verifiedCi)) {
                throw new IllegalArgumentException(MEMBER_MISMATCH_MESSAGE);
            }
        } else {
            String verifiedPhone = MemberIdFindService.normalizePhone(
                    verification.phoneNumber()
            );
            String memberPhone = MemberIdFindService.normalizePhone(
                    member.getPhone()
            );
            if (!member.getName().trim().equals(verification.name().trim())
                    || !memberPhone.equals(verifiedPhone)
                    || memberManagementRepository.existsActiveByCi(verifiedCi)) {
                throw new IllegalArgumentException(MEMBER_MISMATCH_MESSAGE);
            }
            member.registerCi(verifiedCi);
        }

        if (!member.hasPassword()) {
            throw new IllegalArgumentException(MEMBER_MISMATCH_MESSAGE);
        }

        return new PasswordResetVerifyResponse(
                true,
                tokenProvider.create(member),
                tokenProvider.expiresInSeconds()
        );
    }

    @Transactional
    public PasswordResetCompleteResponse complete(
            PasswordResetCompleteRequest request
    ) {
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new IllegalArgumentException(
                    "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }

        PasswordResetTokenProvider.PasswordResetTokenClaims claims =
                tokenProvider.parse(request.resetToken());

        Member member = memberManagementRepository
                .findActiveByIdForPasswordReset(claims.memberId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "비밀번호 재설정 인증이 만료되었거나 유효하지 않습니다."
                ));

        if (!member.isLocalUser() || !member.hasPassword()) {
            throw new IllegalArgumentException(
                    "비밀번호 재설정 인증이 만료되었거나 유효하지 않습니다."
            );
        }

        if (!tokenProvider.matchesCurrentPassword(
                claims.passwordFingerprint(),
                member.getPassword()
        )) {
            throw new IllegalArgumentException(
                    "이미 사용되었거나 유효하지 않은 비밀번호 재설정 인증입니다."
            );
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                member.getPassword()
        )) {
            throw new IllegalArgumentException(
                    "현재 비밀번호와 다른 비밀번호를 입력해 주세요."
            );
        }

        member.changePassword(
                passwordEncoder.encode(request.newPassword())
        );
        refreshTokenService.clear(member);

        return new PasswordResetCompleteResponse(
                "비밀번호가 변경되었습니다. 다시 로그인해 주세요."
        );
    }

    private void validateRecentVerification(String verifiedAtValue) {
        try {
            Instant verifiedAt = Instant.parse(verifiedAtValue);
            Instant now = Instant.now();
            if (verifiedAt.isBefore(now.minus(MAX_VERIFICATION_AGE))
                    || verifiedAt.isAfter(now.plus(FUTURE_CLOCK_TOLERANCE))) {
                throw new IllegalArgumentException(
                        "본인인증 유효시간이 만료되었습니다. 다시 인증해 주세요."
                );
            }
        } catch (NullPointerException | DateTimeException exception) {
            throw new IllegalArgumentException(
                    "본인인증 완료 시각을 확인할 수 없습니다. 다시 인증해 주세요."
            );
        }
    }
}
