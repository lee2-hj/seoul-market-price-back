package com.seoul.market.seoulmarketprice.member.dto.response;

/**
 * 관리자 계정 생성 성공 응답 DTO.
 *
 * <p>비밀번호와 연락처는 노출하지 않고 기본 정보만 반환한다.</p>
 *
 * @param id 생성된 관리자 고유번호
 * @param userId 관리자 로그인 아이디
 * @param name 관리자 이름
 */
public record AdminCreateResponse(
        Long id,
        String userId,
        String name
) {
}
