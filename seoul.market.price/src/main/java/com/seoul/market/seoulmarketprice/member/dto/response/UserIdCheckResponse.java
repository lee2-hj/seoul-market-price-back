package com.seoul.market.seoulmarketprice.member.dto.response;

/**
 * 사용자 아이디 중복 확인 응답 DTO.
 *
 * <p>
 * {@code available}이 true이면 현재 아이디로 가입할 수 있고,
 * false이면 동일한 아이디가 이미 존재한다.
 * 중복 확인 결과는 안내 용도이며, 회원 저장 시 DB UNIQUE 제약으로 다시 검증한다.
 * </p>
 *
 * @param available 아이디를 사용할 수 있으면 true
 */
public record UserIdCheckResponse(
        boolean available
) {
}
