package com.seoul.market.seoulmarketprice.location.repository;

import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 자치구 마스터의 영속성 조회를 담당한다. */
public interface SggMasterRepository extends JpaRepository<SggMaster, Long> {
    /** 프론트엔드 표시 순서가 일정하도록 자치구 이름 오름차순으로 조회한다. */
    List<SggMaster> findAllByOrderBySggNameAsc();

    /** 입력된 자치구 코드가 마스터에 존재하는지 확인한다. */
    boolean existsBySggCode(String sggCode);

    Optional<SggMaster> findBySggCode(String sggCode);

    Optional<SggMaster> findBySggName(String sggName);
}
