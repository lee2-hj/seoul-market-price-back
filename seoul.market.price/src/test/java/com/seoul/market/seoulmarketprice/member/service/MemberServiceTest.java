package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberCreateRequest;
import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberIdCheckRequest;
import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberWithdrawalRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberCreateResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberIdCheckResponse;
import com.seoul.market.seoulmarketprice.member.exception.DuplicateMemberException;
import com.seoul.market.seoulmarketprice.member.repository.MemberManagementRepository;
import com.seoul.market.seoulmarketprice.phoneverification.dto.request.PhoneVerificationConfirmRequest;
import com.seoul.market.seoulmarketprice.phoneverification.dto.response.PhoneVerificationConfirmResponse;
import com.seoul.market.seoulmarketprice.phoneverification.service.PhoneVerificationService;
import com.seoul.market.seoulmarketprice.token.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberManagementRepository memberManagementRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PhoneVerificationService phoneVerificationService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(
                memberManagementRepository,
                passwordEncoder,
                phoneVerificationService,
                refreshTokenService
        );
    }

    @Test
    void createMemberCreatesLocalMemberWithEncodedPassword() {
        MemberCreateRequest request = validRequest();
        when(phoneVerificationService.confirm(
                new PhoneVerificationConfirmRequest("verification-id")
        )).thenReturn(verifiedIdentity());
        when(memberManagementRepository.existsByUserId("market_user"))
                .thenReturn(false);
        when(passwordEncoder.encode("password123!"))
                .thenReturn("encoded-password");
        when(memberManagementRepository.save(any(Member.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MemberCreateResponse response = memberService.createMember(request);

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberManagementRepository).save(captor.capture());
        Member savedMember = captor.getValue();

        assertThat(savedMember.getUserId()).isEqualTo("market_user");
        assertThat(savedMember.getPassword()).isEqualTo("encoded-password");
        assertThat(savedMember.isLocalUser()).isTrue();
        assertThat(savedMember.getCi()).isEqualTo("ci-value");
        assertThat(response.msg()).isNotBlank();
    }

    @Test
    void createMemberRejectsDuplicateUserId() {
        when(phoneVerificationService.confirm(
                new PhoneVerificationConfirmRequest("verification-id")
        )).thenReturn(verifiedIdentity());
        when(memberManagementRepository.existsByUserId("market_user"))
                .thenReturn(true);

        assertThatThrownBy(() -> memberService.createMember(validRequest()))
                .isInstanceOf(DuplicateMemberException.class)
                .hasMessage("이미 사용 중인 아이디입니다.");

        verify(passwordEncoder, never()).encode(any());
        verify(memberManagementRepository, never()).save(any());
    }

    @Test
    void checkUserIdReturnsAvailability() {
        when(memberManagementRepository.existsByUserId("new_user"))
                .thenReturn(false);
        when(memberManagementRepository.existsByUserId("used_user"))
                .thenReturn(true);

        MemberIdCheckResponse available =
                memberService.checkMemberId(new MemberIdCheckRequest("new_user"));
        MemberIdCheckResponse unavailable =
                memberService.checkMemberId(new MemberIdCheckRequest("used_user"));

        assertThat(available.available()).isTrue();
        assertThat(unavailable.available()).isFalse();
    }

    private MemberCreateRequest validRequest() {
        return new MemberCreateRequest(
                "market_user",
                "password123!",
                "서울장터",
                "04524",
                "서울특별시 중구 세종대로 110",
                "1층",
                "010-1234-5678",
                "verification-id",
                "market@example.com",
                (byte) 1,
                (byte) 1,
                (byte) 1,
                "중구",
                "소공동",
                new BigDecimal("37.5642135"),
                new BigDecimal("126.9778292")
        );
    }

    /** 올바른 비밀번호로 탈퇴하면 회원과 모든 Refresh Token이 비활성화되는지 확인한다. */
    @Test
    void withdrawSoftDeletesMemberAndRevokesAllRefreshTokens() {
        Member member = Member.createLocalMember(
                "market_user", "encoded-password", "서울장터",
                null, null, null, "010-1234-5678", null,
                (byte) 1, (byte) 1, (byte) 1, null, null, null, null
        );
        ReflectionTestUtils.setField(member, "id", 1L);
        when(memberManagementRepository.findActiveByIdForWithdrawal(1L))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches("password123!", "encoded-password"))
                .thenReturn(true);

        memberService.withdraw(1L, new MemberWithdrawalRequest("password123!"));

        assertThat(member.isDeleted()).isTrue();
        assertThat(member.getDeleted_at()).isNotNull();
        verify(refreshTokenService).deleteAllByMemberId(1L);
    }

    /** 현재 비밀번호가 다르면 회원 상태와 Refresh Token을 유지하는지 확인한다. */
    @Test
    void withdrawRejectsWrongPasswordWithoutDeletingMember() {
        Member member = Member.createLocalMember(
                "market_user", "encoded-password", "서울장터",
                null, null, null, "010-1234-5678", null,
                (byte) 1, (byte) 1, (byte) 1, null, null, null, null
        );
        when(memberManagementRepository.findActiveByIdForWithdrawal(1L))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong-password", "encoded-password"))
                .thenReturn(false);

        assertThatThrownBy(() -> memberService.withdraw(
                1L, new MemberWithdrawalRequest("wrong-password")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("현재 비밀번호가 올바르지 않습니다.");

        assertThat(member.isDeleted()).isFalse();
        verify(refreshTokenService, never()).deleteAllByMemberId(any());
    }

    private PhoneVerificationConfirmResponse verifiedIdentity() {
        return new PhoneVerificationConfirmResponse(
                true,
                "서울장터",
                "01012345678",
                null,
                null,
                Instant.now().toString(),
                "ci-value"
        );
    }
}
