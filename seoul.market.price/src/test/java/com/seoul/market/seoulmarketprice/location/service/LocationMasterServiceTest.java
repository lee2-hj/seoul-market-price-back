package com.seoul.market.seoulmarketprice.location.service;

import com.seoul.market.seoulmarketprice.location.entity.DongMaster;
import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import com.seoul.market.seoulmarketprice.location.exception.SggNotFoundException;
import com.seoul.market.seoulmarketprice.location.repository.DongMasterRepository;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 자치구 전체 목록과 자치구별 행정동 목록의 조회 규칙을 검증한다. */
class LocationMasterServiceTest {
    private final SggMasterRepository sggRepository = mock(SggMasterRepository.class);
    private final DongMasterRepository dongRepository = mock(DongMasterRepository.class);
    private final LocationMasterService service = new LocationMasterService(sggRepository, dongRepository);

    /** 자치구 목록에 자치구 코드와 이름을 포함한다. */
    @Test
    void returnsAllSggs() {
        SggMaster gangnam = sgg("11680", "강남구");
        when(sggRepository.findAllByOrderBySggNameAsc()).thenReturn(List.of(gangnam));

        var result = service.getSggs();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().sggCd()).isEqualTo("11680");
        assertThat(result.getFirst().sggNm()).isEqualTo("강남구");
    }

    /** 자치구 코드의 공백을 제거하고 해당 자치구의 행정동만 반환한다. */
    @Test
    void returnsDongsForSggCode() {
        DongMaster yeoksam = dong("1168064000", "역삼1동");
        when(sggRepository.existsBySggCode("11680")).thenReturn(true);
        when(dongRepository.findAllBySggSggCodeOrderByDongNameAsc("11680"))
                .thenReturn(List.of(yeoksam));

        var result = service.getDongs(" 11680 ");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().dongCd()).isEqualTo("1168064000");
        assertThat(result.getFirst().dongNm()).isEqualTo("역삼1동");
    }

    /** 마스터에 존재하지 않는 자치구 코드의 조회를 거부한다. */
    @Test
    void rejectsUnknownSggCode() {
        when(sggRepository.existsBySggCode("99999")).thenReturn(false);

        assertThatThrownBy(() -> service.getDongs("99999"))
                .isInstanceOf(SggNotFoundException.class);
    }

    /** 테스트에 필요한 자치구 마스터 대역을 생성한다. */
    private static SggMaster sgg(String code, String name) {
        SggMaster sgg = mock(SggMaster.class);
        when(sgg.getSggCode()).thenReturn(code);
        when(sgg.getSggName()).thenReturn(name);
        return sgg;
    }

    /** 테스트에 필요한 행정동 마스터 대역을 생성한다. */
    private static DongMaster dong(String code, String name) {
        DongMaster dong = mock(DongMaster.class);
        when(dong.getDongCode()).thenReturn(code);
        when(dong.getDongName()).thenReturn(name);
        return dong;
    }
}
