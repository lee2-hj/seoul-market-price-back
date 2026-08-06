package com.seoul.market.seoulmarketprice.board.exception;

/** 게시글 작성자가 아닌 사용자가 변경을 요청했을 때 발생한다. */
public class BoardAccessDeniedException extends RuntimeException {

    /** 게시글 변경 권한이 없음을 나타내는 예외를 생성한다. */
    public BoardAccessDeniedException() { super("게시글을 변경할 권한이 없습니다."); }
}
