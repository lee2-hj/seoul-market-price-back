package com.seoul.market.seoulmarketprice.member.dto.response.admin;

import com.seoul.market.seoulmarketprice.auth.entity.Member;

import java.time.LocalDateTime;

public record AdminMemberDetailResponse(
        Long id,
        String userId,
        String name,
        String email,
        String phone,
        String zipcode,
        String address,
        String addressDetail,
        String preferredRegion,
        String preferredDong,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminMemberDetailResponse from(Member member) {
        return new AdminMemberDetailResponse(
                member.getId(),
                member.getUserId(),
                member.getName(),
                member.getEmail(),
                member.getPhone(),
                member.getZipcode(),
                member.getAddress(),
                member.getAddressDetail(),
                member.getMyGu(),
                member.getMyDong(),
                member.isDeleted() ? "WITHDRAWN" : "ACTIVE",
                member.getCreated_at(),
                member.getUpdated_at()
        );
    }
}
