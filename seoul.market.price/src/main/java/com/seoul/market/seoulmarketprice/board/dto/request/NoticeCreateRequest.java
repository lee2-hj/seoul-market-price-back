package com.seoul.market.seoulmarketprice.board.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 관리자의 공지사항 작성 요청이다. */
public record NoticeCreateRequest(
        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
        String title,
        @NotBlank(message = "내용을 입력해 주세요.")
        String content,
        Boolean visible,
        Boolean pinned
) {
}
