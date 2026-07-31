package com.seoul.market.seoulmarketprice.member.dto.response.admin;

import java.time.LocalDateTime;

/**
 * 관리자 목록 조회 응답 DTO.
 *
 * <p>비밀번호를 제외한 관리자 기본 정보만 반환한다.</p>
 */
public record AdminListResponse(
        String adminId,
        String name,
        String phone,
        String email,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
