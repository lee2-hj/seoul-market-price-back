package com.seoul.market.seoulmarketprice.report.entity;

/** 사용자가 신고할 수 있는 매물·중개 관련 문제 유형이다. */
public enum ReportCategory {
    /** 허위 또는 존재하지 않는 매물 신고. */
    FAKE_LISTING,
    /** 시세를 왜곡하는 가격 정보 신고. */
    PRICE_DISTORTION,
    /** 동일 매물의 중복 등록 신고. */
    DUPLICATE,
    /** 부당한 중개 행위 신고. */
    UNFAIR_BROKERAGE,
    /** 위 분류에 포함되지 않는 기타 신고. */
    OTHER
}
