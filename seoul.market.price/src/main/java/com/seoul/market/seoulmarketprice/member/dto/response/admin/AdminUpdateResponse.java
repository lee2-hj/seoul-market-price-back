package com.seoul.market.seoulmarketprice.member.dto.response.admin;

import java.time.LocalDateTime;

/** 비밀번호를 제외한 관리자 정보 수정 결과 DTO이다. */
public record AdminUpdateResponse(
        Long id,
        String adminId,
        String name,
        String phone,
        String email,
        LocalDateTime updatedAt
) {
}
