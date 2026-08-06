package com.seoul.market.seoulmarketprice.board.exception;

/** 조회 조건에 맞는 활성 게시글이 없을 때 발생한다. */
public class BoardNotFoundException extends RuntimeException {

    /** 게시글을 찾을 수 없음을 나타내는 예외를 생성한다. */
    public BoardNotFoundException() { super("게시글을 찾을 수 없습니다."); }
}
