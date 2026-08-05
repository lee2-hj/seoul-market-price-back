package com.seoul.market.seoulmarketprice.comment.exception;
/** 댓글이 없거나 이미 삭제된 경우 발생한다. */
public class CommentNotFoundException extends RuntimeException {

    public CommentNotFoundException() {
        super("댓글을 찾을 수 없습니다.");
    }
}
