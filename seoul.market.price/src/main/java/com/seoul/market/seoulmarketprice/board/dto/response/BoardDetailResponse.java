package com.seoul.market.seoulmarketprice.board.dto.response;
import com.seoul.market.seoulmarketprice.board.entity.PostType;
import java.time.LocalDateTime;

/** 게시글 본문과 노출 상태를 포함하는 상세 응답이다. */
public record BoardDetailResponse(
        Long id,
        PostType postType,
        String title,
        String content,
        String userId,
        Long memberId,
        int viewCount,
        boolean visible,
        boolean pinned,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
