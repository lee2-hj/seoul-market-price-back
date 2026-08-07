package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberCreateRequest;
import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberIdCheckRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberCreateResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberIdCheckResponse;
import com.seoul.market.seoulmarketprice.member.exception.DuplicateMemberException;
import com.seoul.market.seoulmarketprice.member.repository.MemberManagementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(
                memberManagementRepository,
                passwordEncoder
        );
    }

    @Test
    void createMemberCreatesLocalMemberWithEncodedPassword() {
        MemberCreateRequest request = validRequest();
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
        assertThat(response.msg()).isNotBlank();
    }

    @Test
    void createMemberRejectsDuplicateUserId() {
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
                "market@example.com",
                (byte) 1,
                (byte) 1,
                (byte) 1,
                "서울"
        );
    }
}
