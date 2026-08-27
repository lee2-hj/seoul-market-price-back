package com.seoul.market.seoulmarketprice.comment.dto.response;

import com.seoul.market.seoulmarketprice.comment.entity.BoardType;

import java.time.LocalDateTime;

/** 마이페이지의 내 댓글 목록 한 건과 댓글이 작성된 원문 정보를 나타낸다. */
public record MyCommentResponse(
        Long id,
        Long parentId,
        BoardType boardType,
        Long postId,
        String postTitle,
        String name,
        String content,
        boolean visible,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
