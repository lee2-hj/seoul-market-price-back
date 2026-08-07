package com.seoul.market.seoulmarketprice.qna.exception;

/** 작성자 전용 Q&A 변경 작업의 권한이 없을 때 발생한다. */
public class QnaAccessDeniedException extends RuntimeException {
    /** 공통 사용자 메시지로 예외를 생성한다. */
    public QnaAccessDeniedException() {
        super("Q&A 게시글에 접근할 권한이 없습니다.");
    }
}
