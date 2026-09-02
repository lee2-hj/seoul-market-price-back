package com.seoul.market.seoulmarketprice.board.comment.dto.request;
import jakarta.validation.constraints.NotNull;
/** 관리자의 댓글 노출 상태 변경 요청이다. */
public record CommentVisibilityRequest(
        @NotNull(message = "노출 여부를 입력해 주세요.")
        Boolean visible
) {
}
