package com.seoul.market.seoulmarketprice.auth.dto.response;

/**
 * 관리자 로그인 성공 시 클라이언트에게 반환하는 응답 DTO이다.
 *
 * <p>
 * 로그인에 성공하면 Access Token과
 * 로그인한 관리자의 기본 정보를 함께 반환한다.
 * </p>
 *
 * @param accessToken 인증이 완료된 관리자에게 발급되는 Access Token
 * @param adminId 로그인한 관리자 아이디
 * @param name 관리자 이름
 */
public record AdminLoginResponse(

        String accessToken,

        String adminId,

        String name

) {
}