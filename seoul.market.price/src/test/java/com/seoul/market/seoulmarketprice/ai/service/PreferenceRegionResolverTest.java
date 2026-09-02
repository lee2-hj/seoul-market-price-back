package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.auth.repository.MemberRepository;
import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PreferenceRegionResolverTest {

    @Test
    void replacesPreferenceRegionExpressionWithSavedDistrictName() {
        MemberRepository members = mock(MemberRepository.class);
        SggMasterRepository sggs = mock(SggMasterRepository.class);
        Member member = mock(Member.class);
        SggMaster yongsan = mock(SggMaster.class);
        when(members.findById(7L)).thenReturn(Optional.of(member));
        when(member.getMyGu()).thenReturn("11170");
        when(sggs.findBySggCode("11170")).thenReturn(Optional.of(yongsan));
        when(yongsan.getSggName()).thenReturn("용산구");

        var resolution = new PreferenceRegionResolver(members, sggs)
                .resolve("내 선호지역 10억대 아파트 알려줘", 7L);

        assertThat(resolution.status()).isEqualTo(PreferenceRegionResolver.Status.RESOLVED);
        assertThat(resolution.question()).isEqualTo("용산구 10억대 아파트 알려줘");
    }

    @Test
    void returnsUnavailableWithoutAuthenticatedMemberAndDoesNotQueryMemberData() {
        MemberRepository members = mock(MemberRepository.class);
        SggMasterRepository sggs = mock(SggMasterRepository.class);

        var resolution = new PreferenceRegionResolver(members, sggs)
                .resolve("내 관심지역 아파트 알려줘", null);

        assertThat(resolution.status()).isEqualTo(PreferenceRegionResolver.Status.PREFERENCE_UNAVAILABLE);
        verifyNoInteractions(members, sggs);
    }

    @Test
    void keepsExistingQuestionUntouchedWhenPreferenceExpressionIsAbsent() {
        MemberRepository members = mock(MemberRepository.class);
        SggMasterRepository sggs = mock(SggMasterRepository.class);

        var resolution = new PreferenceRegionResolver(members, sggs)
                .resolve("강남구 10억대 아파트 알려줘", 7L);

        assertThat(resolution.status()).isEqualTo(PreferenceRegionResolver.Status.UNCHANGED);
        assertThat(resolution.question()).isEqualTo("강남구 10억대 아파트 알려줘");
        verifyNoInteractions(members, sggs);
    }
}
