package com.seoul.market.seoulmarketprice.member.dto.response.admin;

import java.util.List;

/**
 * 관리자 목록 페이징 응답 DTO.
 */
public record AdminPageResponse(
        List<AdminListResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
