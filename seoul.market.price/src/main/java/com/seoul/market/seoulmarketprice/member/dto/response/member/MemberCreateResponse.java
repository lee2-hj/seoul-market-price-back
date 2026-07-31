package com.seoul.market.seoulmarketprice.member.dto.response.member;

/**
 * 일반 회원가입 성공 응답 DTO.
 *
 * <p>
 * 생성된 Member 엔티티 전체를 반환하지 않고,
 * 가입 완료 화면에 필요한 최소 정보만 전달한다.
 * 비밀번호와 주소, 연락처는 응답에 포함하지 않는다.
 * </p>
 *
 * @param msg     사용자 이름
 */
public record MemberCreateResponse(
        String msg
) {
}
