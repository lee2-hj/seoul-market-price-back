package com.seoul.market.seoulmarketprice.faq.dto.response;

import java.time.LocalDateTime;

/** 관리자 화면에 노출 상태와 수정 시각을 함께 전달하는 FAQ 응답이다. */
public record AdminFaqResponse(
        Long id,
        String question,
        String answer,
        String category,
        String writerName,
        int displayOrder,
        boolean visible,
        int viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
