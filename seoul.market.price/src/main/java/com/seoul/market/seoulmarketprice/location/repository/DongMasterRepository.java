package com.seoul.market.seoulmarketprice.location.repository;

import com.seoul.market.seoulmarketprice.location.entity.DongMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 행정동 마스터의 영속성 조회를 담당한다. */
public interface DongMasterRepository extends JpaRepository<DongMaster, Long> {
    /** 자치구 코드에 속한 행정동만 행정동 이름 오름차순으로 조회한다. */
    List<DongMaster> findAllBySggSggCodeOrderByDongNameAsc(String sggCode);
}
