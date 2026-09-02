package com.seoul.market.seoulmarketprice.board.comment.exception;
/** 댓글 작성자가 아닌 사용자가 변경을 요청한 경우 발생한다. */
public class CommentAccessDeniedException extends RuntimeException {

    public CommentAccessDeniedException() {
        super("댓글을 변경할 권한이 없습니다.");
    }
}
