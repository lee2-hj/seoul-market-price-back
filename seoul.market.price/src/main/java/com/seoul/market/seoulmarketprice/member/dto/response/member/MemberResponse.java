package com.seoul.market.seoulmarketprice.member.dto.response.member;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.auth.entity.UserType;
import com.seoul.market.seoulmarketprice.member.service.DistrictPreferenceResolver;

import java.time.LocalDateTime;
import java.math.BigDecimal;

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
        /** 사용자가 선택한 선호 자치구. */
        String myGu,
        String myGuCode,
        /** myGu, 가입 주소, 중구 순서로 결정한 헤더 표시 자치구. */
        String preferredDistrict,
        /** 사용자가 선택한 선호 행정동. */
        String myDong,
        @JsonProperty("isLocationAgreed") boolean isLocationAgreed,
        /** 선호 위치의 위도. */
        BigDecimal latitude,
        /** 선호 위치의 경도. */
        BigDecimal longitude,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
    /** 회원 엔티티에서 비밀번호와 CI를 제외한 화면용 응답을 생성한다. */
    public static MemberResponse from(Member member) {
        String preferredGu = member.getPreferredSgg() == null
                ? member.getMyGu()
                : member.getPreferredSgg().getSggName();
        String preferredGuCode = member.getPreferredSgg() == null
                ? null
                : member.getPreferredSgg().getSggCode();
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
                preferredGu,
                preferredGuCode,
                DistrictPreferenceResolver.resolve(
                        preferredGu,
                        member.getAddress()
                ),
                member.getMyDong(),
                Byte.valueOf((byte) 1).equals(member.getIsLocationAgreed()),
                member.getLatitude(),
                member.getLongitude(),
                member.getCreated_at(),
                member.getUpdated_at(),
                member.getDeleted_at()
        );
    }
}
