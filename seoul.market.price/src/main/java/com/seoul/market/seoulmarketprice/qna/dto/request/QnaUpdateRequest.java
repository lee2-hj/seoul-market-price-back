package com.seoul.market.seoulmarketprice.qna.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 작성자가 자신의 Q&A 질문을 부분 수정하는 요청이다.
 *
 * @param title 변경할 제목
 * @param questionContent 변경할 질문 본문
 * @param publicQuestion 변경할 공개 여부
 */
public record QnaUpdateRequest(
        @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
        String title,
        String questionContent,
        Boolean publicQuestion
) {
}
