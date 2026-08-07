package com.seoul.market.seoulmarketprice.qna.exception;

/** 존재하지 않거나 현재 접근 조건에서 조회할 수 없는 Q&A 요청 예외이다. */
public class QnaNotFoundException extends RuntimeException {
    /** 공통 사용자 메시지로 예외를 생성한다. */
    public QnaNotFoundException() {
        super("Q&A 게시글을 찾을 수 없습니다.");
    }
}
