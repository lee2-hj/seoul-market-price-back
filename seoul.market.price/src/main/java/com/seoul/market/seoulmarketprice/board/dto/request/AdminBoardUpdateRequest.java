package com.seoul.market.seoulmarketprice.board.dto.request;
import jakarta.validation.constraints.Size;
/** 관리자의 게시글 선택 수정 요청이다. */
public record AdminBoardUpdateRequest(
        @Size(min = 1, max = 200, message = "제목은 1자 이상 200자 이하여야 합니다.")
        String title,
        @Size(min = 1, message = "내용은 비어 있을 수 없습니다.")
        String content,
        Boolean visible,
        Boolean pinned
) {
}
