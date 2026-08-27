package com.seoul.market.seoulmarketprice.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 행정동의 표준 코드와 이름 및 소속 자치구를 관리한다. */
@Entity
@Getter
@Table(name = "tb_dong_master")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DongMaster {
    /** 행정동 마스터의 내부 식별자이다. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 외부 행정구역 데이터와 연계할 때 사용하는 행정동 코드이다. */
    @Column(name = "dong_cd", nullable = false, unique = true, length = 10)
    private String dongCode;

    /** 사용자 화면에 표시하는 행정동 이름이다. */
    @Column(name = "dong_nm", nullable = false, length = 50)
    private String dongName;

    /** 행정동이 속한 상위 자치구이다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sgg_id", nullable = false)
    private SggMaster sgg;
}
