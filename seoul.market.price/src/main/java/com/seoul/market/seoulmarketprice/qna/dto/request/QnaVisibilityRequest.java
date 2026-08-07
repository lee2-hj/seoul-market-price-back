package com.seoul.market.seoulmarketprice.qna.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 관리자가 Q&A 공개 여부를 변경하는 요청이다.
 *
 * @param publicQuestion 공개 여부
 */
public record QnaVisibilityRequest(
        @NotNull(message = "공개 여부를 입력해 주세요.")
        Boolean publicQuestion
) {
}
