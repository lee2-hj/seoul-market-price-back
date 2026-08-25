package com.seoul.market.seoulmarketprice.board.dto.response;

import java.util.List;

public record AdminBoardPageResponse(
        List<AdminBoardListResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
