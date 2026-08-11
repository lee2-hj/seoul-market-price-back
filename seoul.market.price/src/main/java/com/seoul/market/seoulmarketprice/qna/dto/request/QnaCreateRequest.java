package com.seoul.market.seoulmarketprice.qna.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 로그인 사용자의 Q&A 질문 등록 요청이다.
 *
 * @param title 질문 제목
 * @param questionContent 질문 본문
 * @param publicQuestion 공개 여부, null이면 공개로 처리
 */
public record QnaCreateRequest(
        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
        String title,
        @NotBlank(message = "질문 내용을 입력해 주세요.")
        String questionContent,
        Boolean publicQuestion
) {
}
