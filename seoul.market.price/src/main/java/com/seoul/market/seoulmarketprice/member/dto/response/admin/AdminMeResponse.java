package com.seoul.market.seoulmarketprice.member.dto.response.admin;

import com.seoul.market.seoulmarketprice.auth.entity.Role;

/**
 * 현재 로그인한 관리자 자신의 기본 정보 응답 DTO.
 *
 * @param id      관리자 고유번호
 * @param adminId 관리자 로그인 아이디
 * @param name    관리자 이름
 * @param role    관리자 권한(ADMIN 또는 MASTER)
 */
public record AdminMeResponse(
        Long id,
        String adminId,
        String name,
        Role role
) {
}
