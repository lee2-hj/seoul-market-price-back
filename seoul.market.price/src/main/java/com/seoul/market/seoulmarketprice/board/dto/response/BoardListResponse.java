package com.seoul.market.seoulmarketprice.board.dto.response;
import com.seoul.market.seoulmarketprice.board.entity.PostType;
import java.time.LocalDateTime;

/** 게시글 목록 한 건을 표현하는 응답이다. */
public record BoardListResponse(
        Long id,
        PostType postType,
        String title,
        String userId,
        String writerName,
        Long memberId,
        int viewCount,
        boolean pinned,
        LocalDateTime createdAt
) {
}
