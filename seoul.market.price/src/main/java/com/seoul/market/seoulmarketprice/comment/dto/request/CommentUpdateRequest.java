package com.seoul.market.seoulmarketprice.comment.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/** 작성자 본인의 댓글 내용 수정 요청이다. */
public record CommentUpdateRequest(
        @NotBlank(message = "댓글 내용을 입력해 주세요.")
        @Size(max = 2000, message = "댓글은 2000자 이하여야 합니다.")
        String content
) {
}
