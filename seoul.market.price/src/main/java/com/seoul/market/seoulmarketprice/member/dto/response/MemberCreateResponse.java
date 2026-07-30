package com.seoul.market.seoulmarketprice.member.dto.response;

/**
 * 일반 회원가입 성공 응답 DTO.
 *
 * <p>
 * 생성된 Member 엔티티 전체를 반환하지 않고,
 * 가입 완료 화면에 필요한 최소 정보만 전달한다.
 * 비밀번호와 주소, 연락처는 응답에 포함하지 않는다.
 * </p>
 *
 * @param memberId 생성된 회원의 고유번호
 * @param userId   로그인 아이디
 * @param name     사용자 이름
 */
public record MemberCreateResponse(
        Long memberId,
        String userId,
        String name
) {
}
