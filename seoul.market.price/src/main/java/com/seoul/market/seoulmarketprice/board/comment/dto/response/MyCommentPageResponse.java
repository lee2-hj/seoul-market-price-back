package com.seoul.market.seoulmarketprice.board.comment.dto.response;

import java.util.List;

/** 내 댓글 목록과 현재 페이지 정보를 함께 전달한다. */
public record MyCommentPageResponse(
        List<MyCommentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
