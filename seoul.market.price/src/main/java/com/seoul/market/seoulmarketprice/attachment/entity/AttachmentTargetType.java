package com.seoul.market.seoulmarketprice.attachment.entity;

/** 첨부파일이 속한 게시판 종류와 MinIO 객체 경로 접두사를 정의한다. */
public enum AttachmentTargetType {
    /** 일반 게시판과 관리자 공지사항 첨부파일. */
    BOARD("board"),
    /** 사용자 Q&A 게시판 첨부파일. */
    QNA_BOARD("qna_board");

    /** MinIO 객체 키의 최상위 가상 디렉터리명. */
    private final String objectPrefix;

    /** 게시판 종류별 객체 키 접두사를 설정한다. */
    AttachmentTargetType(String objectPrefix) {
        this.objectPrefix = objectPrefix;
    }

    /** MinIO 객체 키를 생성할 때 사용할 접두사를 반환한다. */
    public String objectPrefix() {
        return objectPrefix;
    }
}
