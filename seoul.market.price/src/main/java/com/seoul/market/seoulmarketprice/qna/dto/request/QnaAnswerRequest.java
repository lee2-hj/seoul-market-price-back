package com.seoul.market.seoulmarketprice.qna.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 관리자가 Q&A 답변을 등록하거나 수정하는 요청이다.
 *
 * @param answerContent 저장할 답변 본문
 */
public record QnaAnswerRequest(
        @NotBlank(message = "답변 내용을 입력해 주세요.")
        String answerContent
) {
}
