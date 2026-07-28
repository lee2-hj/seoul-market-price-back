package com.seoul.market.seoulmarketprice.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO
 *
 * 클라이언트(React)에서 전달하는 로그인 정보를 담는다.
 *
 * Entity를 직접 사용하는 대신 DTO(record)를 사용하여
 * 필요한 데이터만 전달받는다.
 */
public record LoginRequest(

        /**
         * 로그인 아이디
         */
        @NotBlank(message = "아이디는 필수입니다.")
        String userId,

        /**
         * 로그인 비밀번호
         */
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password

) {
}