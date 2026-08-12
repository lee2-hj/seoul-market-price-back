package com.seoul.market.seoulmarketprice.report.entity;

/** 신고 접수 이후 관리자 처리 진행 상태이다. */
public enum ReportStatus {
    /** 신고가 최초 접수된 상태. */
    RECEIVED,
    /** 관리자가 내용을 확인하고 처리 중인 상태. */
    IN_PROGRESS,
    /** 신고 처리가 정상적으로 완료된 상태. */
    RESOLVED,
    /** 신고 사유가 인정되지 않아 반려된 상태. */
    REJECTED
}
