package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.NaturalSearchErrorCode;
import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.auth.repository.MemberRepository;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NaturalLanguageSearchServicePreferenceTest {

    @Test
    void returnsGuidanceBeforeQuestionAnalysisWhenPreferenceIsNotConfigured() {
        MemberRepository members = mock(MemberRepository.class);
        Member member = mock(Member.class);
        when(members.findById(7L)).thenReturn(Optional.of(member));
        when(member.getMyGu()).thenReturn(null);
        QuestionAnalysisService analyzer = mock(QuestionAnalysisService.class);
        NaturalLanguageSearchService service = service(analyzer,
                new PreferenceRegionResolver(members, mock(SggMasterRepository.class)));

        var response = service.search("내 선호지역 10억대 아파트 알려줘", 7L);

        assertThat(response.errorCode()).isEqualTo(NaturalSearchErrorCode.MISSING_REGION);
        assertThat(response.message()).isEqualTo(PreferenceRegionResolver.PREFERENCE_REGION_REQUIRED_MESSAGE);
        verifyNoInteractions(analyzer);
    }

    private NaturalLanguageSearchService service(QuestionAnalysisService analyzer,
                                                 PreferenceRegionResolver preferenceResolver) {
        return new NaturalLanguageSearchService(null, null, analyzer, null, null, null,
                null, null, null, null, preferenceResolver);
    }
}
