package com.seoul.market.seoulmarketprice.faq.dto.response;

import java.time.LocalDateTime;

/** 사용자 화면에 공개하는 FAQ 응답이다. */
public record FaqPublicResponse(
        Long id,
        String question,
        String answer,
        String category,
        int displayOrder,
        int viewCount,
        LocalDateTime createdAt
) {
}
