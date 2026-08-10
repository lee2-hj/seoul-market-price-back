package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberIdFindRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberIdFindResponse;
import com.seoul.market.seoulmarketprice.member.repository.MemberManagementRepository;
import com.seoul.market.seoulmarketprice.phoneverification.dto.request.PhoneVerificationConfirmRequest;
import com.seoul.market.seoulmarketprice.phoneverification.dto.response.PhoneVerificationConfirmResponse;
import com.seoul.market.seoulmarketprice.phoneverification.service.PhoneVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberIdFindServiceTest {

    @Mock
    private PhoneVerificationService phoneVerificationService;

    @Mock
    private MemberManagementRepository memberManagementRepository;

    private MemberIdFindService service;

    @BeforeEach
    void setUp() {
        service = new MemberIdFindService(
                phoneVerificationService,
                memberManagementRepository
        );
    }

    @Test
    void returnsOnlyMaskedUserIdAfterPassVerification() {
        Member member = Member.createLocalMember(
                "seouluser01", "encoded", "홍길동", null, null, null,
                "010-1234-5678", null, (byte) 1, (byte) 1, (byte) 1, null
        );
        when(phoneVerificationService.confirm(
                new PhoneVerificationConfirmRequest("verification-id")
        )).thenReturn(new PhoneVerificationConfirmResponse(
                true, " 홍길동 ", "+82 10-1234-5678", null, null
        ));
        when(memberManagementRepository.findActiveLocalMembersByVerifiedIdentity(
                "홍길동", "01012345678"
        )).thenReturn(List.of(member));

        MemberIdFindResponse response = service.find(
                new MemberIdFindRequest("verification-id")
        );

        assertThat(response.found()).isTrue();
        assertThat(response.maskedUserIds()).containsExactly("seo****01");
        assertThat(response.maskedUserIds()).doesNotContain("seouluser01");
    }

    @Test
    void returnsEmptyResultWhenNoMemberMatches() {
        when(phoneVerificationService.confirm(
                new PhoneVerificationConfirmRequest("verification-id")
        )).thenReturn(new PhoneVerificationConfirmResponse(
                true, "홍길동", "01012345678", null, null
        ));
        when(memberManagementRepository.findActiveLocalMembersByVerifiedIdentity(
                "홍길동", "01012345678"
        )).thenReturn(List.of());

        MemberIdFindResponse response = service.find(
                new MemberIdFindRequest("verification-id")
        );

        assertThat(response.found()).isFalse();
        assertThat(response.maskedUserIds()).isEmpty();
    }

    @Test
    void masksShortUserIdsWithoutExposingThem() {
        assertThat(MemberIdFindService.maskUserId("a")).isEqualTo("*");
        assertThat(MemberIdFindService.maskUserId("ab")).isEqualTo("**");
        assertThat(MemberIdFindService.maskUserId("abc")).isEqualTo("a**");
        assertThat(MemberIdFindService.maskUserId("abcd")).isEqualTo("a**d");
        assertThat(MemberIdFindService.maskUserId("abcde")).isEqualTo("ab**e");
    }
}
