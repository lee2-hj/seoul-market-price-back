package com.seoul.market.seoulmarketprice.board.exception;
public class BoardAccessDeniedException extends RuntimeException {
    public BoardAccessDeniedException() { super("게시글을 변경할 권한이 없습니다."); }
}
