package com.seoul.market.seoulmarketprice.location.service;

import com.seoul.market.seoulmarketprice.location.dto.DongResponse;
import com.seoul.market.seoulmarketprice.location.dto.DongRegionResponse;
import com.seoul.market.seoulmarketprice.location.entity.DongMaster;
import com.seoul.market.seoulmarketprice.location.dto.SggResponse;
import com.seoul.market.seoulmarketprice.location.exception.SggNotFoundException;
import com.seoul.market.seoulmarketprice.location.repository.DongMasterRepository;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;

/** 자치구와 행정동 마스터의 프론트엔드 선택 목록 조회를 담당한다. */
@Service
public class LocationMasterService {
    private final SggMasterRepository sggMasterRepository;
    private final DongMasterRepository dongMasterRepository;

    public LocationMasterService(
            SggMasterRepository sggMasterRepository,
            DongMasterRepository dongMasterRepository
    ) {
        this.sggMasterRepository = sggMasterRepository;
        this.dongMasterRepository = dongMasterRepository;
    }

    /** 모든 자치구를 이름 오름차순으로 조회한다. */
    @Transactional(readOnly = true)
    public List<SggResponse> getSggs() {
        return sggMasterRepository.findAllByOrderBySggNameAsc()
                .stream()
                .map(sgg -> new SggResponse(sgg.getSggCode(), sgg.getSggName()))
                .toList();
    }

    /** 자치구 코드에 속한 모든 행정동을 이름 오름차순으로 조회한다. */
    @Transactional(readOnly = true)
    public List<DongResponse> getDongs(String sggCode) {
        String normalizedSggCode = normalizeSggCode(sggCode);
        if (!sggMasterRepository.existsBySggCode(normalizedSggCode)) {
            throw new SggNotFoundException(normalizedSggCode);
        }

        return dongMasterRepository.findAllBySggSggCodeOrderByDongNameAsc(normalizedSggCode)
                .stream()
                .map(dong -> new DongResponse(dong.getDongCode(), dong.getDongName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DongRegionResponse> resolveDongs(String dong1, String dong2) {
        List<DongRegionResponse> result = new ArrayList<>();
        for (String name : List.of(dong1, dong2)) {
            DongMaster dong = dongMasterRepository.findAllByDongName(name).stream().findFirst()
                    .or(() -> dongMasterRepository.findAll().stream()
                            .filter(candidate -> candidate.getDongName().contains(name.replaceAll("[0-9]", "")))
                            .findFirst())
                    .orElseThrow(() -> new IllegalArgumentException(name + "을(를) 찾을 수 없습니다."));
            result.add(new DongRegionResponse(dong.getDongName(), dong.getDongCode(), dong.getSgg().getSggName(), dong.getSgg().getSggCode()));
        }
        return result;
    }

    /** 요청받은 자치구 코드의 앞뒤 공백을 제거하고 빈 값 입력을 거부한다. */
    private String normalizeSggCode(String sggCode) {
        if (sggCode == null || sggCode.isBlank()) {
            throw new IllegalArgumentException("자치구 코드는 필수입니다.");
        }
        return sggCode.trim();
    }
}
