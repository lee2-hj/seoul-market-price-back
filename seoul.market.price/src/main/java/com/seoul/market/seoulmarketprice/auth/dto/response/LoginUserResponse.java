package com.seoul.market.seoulmarketprice.auth.dto.response;

/**
 * 현재 로그인한 사용자 정보를 반환하는 응답 DTO이다.
 *
 * <p>
 * Access Token 인증에 성공한 사용자의 기본 정보를
 * React에 전달할 때 사용한다.
 * </p>
 *
 * <p>
 * Member 엔티티를 직접 반환하지 않고,
 * 화면에 필요한 정보만 record DTO로 전달한다.
 * </p>
 *
 * @param memberId 회원 고유번호
 * @param userId   로그인 아이디
 */
public record LoginUserResponse(
        Long memberId,
        String userId
) {
}