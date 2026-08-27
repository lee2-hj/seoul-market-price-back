package com.seoul.market.seoulmarketprice.faq.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** 관리자의 FAQ 선택 수정 요청이다. */
public record FaqUpdateRequest(
        @Size(min = 1, max = 300, message = "질문은 1자 이상 300자 이하여야 합니다.")
        String question,
        @Size(min = 1, message = "답변은 비어 있을 수 없습니다.")
        String answer,
        @Size(max = 50, message = "카테고리는 50자 이하여야 합니다.")
        String category,
        @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다.")
        Integer displayOrder,
        Boolean visible
) {
}
