package com.seoul.market.seoulmarketprice.auth.dto.response;

/**
 * 로그인 성공 응답 DTO
 *
 * Refresh Token은 HttpOnly Cookie로 전달하므로
 * Response Body에는 포함하지 않는다.
 */
public record LoginResponse(

        /**
         * 인증이 필요한 API 호출 시 사용하는 JWT
         */
        String accessToken,

        /**
         * 회원 PK
         */
        Long memberId,

        /**
         * 로그인 아이디
         */
        String userId,

        /**
         * 사용자 이름
         */
        String name

) {
}