package com.seoul.market.seoulmarketprice.member.dto.response.admin;

import java.util.List;

/** 관리자 일반 회원 목록 페이지 응답 DTO. */
public record AdminMemberPageResponse(
        List<AdminMemberListResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
