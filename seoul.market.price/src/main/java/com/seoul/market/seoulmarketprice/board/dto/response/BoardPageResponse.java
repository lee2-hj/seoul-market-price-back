package com.seoul.market.seoulmarketprice.board.dto.response;
import java.util.List;

/** 게시글 목록과 페이지 메타데이터를 전달하는 응답이다. */
public record BoardPageResponse(
        List<BoardListResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
