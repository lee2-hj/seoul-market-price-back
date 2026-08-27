package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RankingRegionResolverTest {
    @Test
    void normalizesDistrictAliasWithoutGuSuffix() {
        SggMaster gangnam = district("강남구", "11680");
        SggMasterRepository repository = mock(SggMasterRepository.class);
        when(repository.findAllByOrderBySggNameAsc()).thenReturn(List.of(gangnam));

        var result = new RankingRegionResolver(repository, mock(LocationMasterService.class))
                .resolve("강남에서 제일 싼 아파트 알려줘");

        assertThat(result.allSeoul()).isFalse();
        assertThat(result.name()).isEqualTo("강남구");
        assertThat(result.sggCode()).isEqualTo("11680");
    }

    @Test
    void permitsAllSeoulOnlyWhenUserExplicitlyRequestsIt() {
        var result = new RankingRegionResolver(mock(SggMasterRepository.class), mock(LocationMasterService.class))
                .resolve("서울 전체에서 제일 싼 아파트 알려줘");

        assertThat(result.allSeoul()).isTrue();
        assertThat(result.name()).isEqualTo("서울 전체");
    }

    @Test
    void suggestsClosestDistrictForTypo() {
        SggMasterRepository repository = mock(SggMasterRepository.class);
        SggMaster gangnam = district("강남구", "11680");
        when(repository.findAllByOrderBySggNameAsc()).thenReturn(List.of(gangnam));

        assertThatThrownBy(() -> new RankingRegionResolver(repository, mock(LocationMasterService.class))
                .resolve("강암에서 제일 싼 아파트 알려줘"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("강남구를 의미하셨나요?");
    }

    @Test
    void rejectsUnknownRegionInsteadOfFallingBackToAllSeoul() {
        SggMasterRepository repository = mock(SggMasterRepository.class);
        SggMaster gangnam = district("강남구", "11680");
        when(repository.findAllByOrderBySggNameAsc()).thenReturn(List.of(gangnam));

        assertThatThrownBy(() -> new RankingRegionResolver(repository, mock(LocationMasterService.class))
                .resolve("없는지역에서 제일 싼 아파트 알려줘"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("입력한 지역을 찾을 수 없습니다. 자치구 또는 자치동 이름을 입력해주세요.");
    }

    private SggMaster district(String name, String code) {
        SggMaster sgg = mock(SggMaster.class);
        when(sgg.getSggName()).thenReturn(name);
        when(sgg.getSggCode()).thenReturn(code);
        return sgg;
    }
}
