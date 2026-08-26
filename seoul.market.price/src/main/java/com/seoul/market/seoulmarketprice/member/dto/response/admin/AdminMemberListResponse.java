package com.seoul.market.seoulmarketprice.member.dto.response.admin;

import com.seoul.market.seoulmarketprice.auth.entity.Member;

import java.time.LocalDateTime;

/** 관리자 일반 회원 목록 응답 DTO. 민감한 인증 정보는 포함하지 않는다. */
public record AdminMemberListResponse(
        Long id,
        String userId,
        String name,
        String email,
        String phone,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminMemberListResponse from(Member member) {
        return new AdminMemberListResponse(
                member.getId(),
                member.isDeleted() ? "" : member.getUserId(),
                member.getName(),
                member.getEmail(),
                member.isDeleted() ? "" : member.getPhone(),
                member.isDeleted() ? "WITHDRAWN" : "ACTIVE",
                member.getCreated_at(),
                member.getUpdated_at()
        );
    }
}
