package com.seoul.market.seoulmarketprice.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 로그인 요청 정보를 전달하는 DTO이다.
 *
 * <p>
 * 프론트엔드 또는 Swagger에서 전달한
 * 관리자 아이디와 비밀번호를 받는다.
 * </p>
 *
 * <p>
 * Controller에서는 Admin 엔티티를 직접 받지 않고,
 * 이 Request DTO를 통해 로그인 정보를 전달받는다.
 * </p>
 *
 * @param adminId 관리자 로그인 아이디
 * @param password 관리자 로그인 비밀번호
 */
public record AdminLoginRequest(

        /**
         * 관리자 로그인 아이디.
         *
         * <p>
         * null, 빈 문자열, 공백만 있는 값은 허용하지 않는다.
         * </p>
         */
        @NotBlank(message = "관리자 아이디는 필수입니다.")
        String adminId,

        /**
         * 관리자 로그인 비밀번호.
         *
         * <p>
         * 입력값은 평문으로 전달되며,
         * Service에서 DB의 BCrypt 암호문과 비교한다.
         * </p>
         */
        @NotBlank(message = "관리자 비밀번호는 필수입니다.")
        String password
) {
}