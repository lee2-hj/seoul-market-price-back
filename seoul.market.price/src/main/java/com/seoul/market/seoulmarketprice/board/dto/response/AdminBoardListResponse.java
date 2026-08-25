package com.seoul.market.seoulmarketprice.board.dto.response;

import com.seoul.market.seoulmarketprice.board.entity.PostType;

import java.time.LocalDateTime;

public record AdminBoardListResponse(
        Long id,
        Long boardId,
        PostType postType,
        String title,
        String content,
        String userId,
        String writerName,
        Long memberId,
        int viewCount,
        boolean visible,
        boolean pinned,
        LocalDateTime createdAt
) {
}
