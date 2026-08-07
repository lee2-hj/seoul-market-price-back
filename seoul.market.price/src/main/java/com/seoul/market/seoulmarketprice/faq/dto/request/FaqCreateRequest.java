package com.seoul.market.seoulmarketprice.faq.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 관리자의 FAQ 등록 요청이다. */
public record FaqCreateRequest(
        @NotBlank(message = "질문을 입력해 주세요.")
        @Size(max = 300, message = "질문은 300자 이하여야 합니다.")
        String question,
        @NotBlank(message = "답변을 입력해 주세요.")
        String answer,
        @Size(max = 50, message = "카테고리는 50자 이하여야 합니다.")
        String category,
        @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다.")
        Integer displayOrder,
        Boolean visible
) {
}
