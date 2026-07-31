package com.seoul.market.seoulmarketprice.member.dto.response.member;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.auth.entity.UserType;

import java.time.LocalDateTime;

/**
 * 현재 로그인한 회원 정보를 반환하는 응답 DTO.
 *
 * <p>
 * Access Token 인증에 성공한 회원의 기본 정보를
 * React에 전달할 때 사용한다.
 * </p>
 *
 * <p>
 * Member 엔티티를 직접 반환하지 않고,
 * 화면에 필요한 정보만 record DTO로 전달한다.
 * </p>
 *
 * @param memberId 인증된 회원의 고유번호
 * @param userId   인증된 회원의 로그인 아이디
 */
public record MemberResponse(
        Long memberId,
        String userId,
        String name,
        String zipcode,
        String address,
        String addressDetail,
        String phone,
        String email,
        String socialId,
        UserType userType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getUserId(),
                member.getName(),
                member.getZipcode(),
                member.getAddress(),
                member.getAddressDetail(),
                member.getPhone(),
                member.getEmail(),
                member.getSocialId(),
                member.getUserType(),
                member.getCreated_at(),
                member.getUpdated_at(),
                member.getDeleted_at()
        );
    }
}
