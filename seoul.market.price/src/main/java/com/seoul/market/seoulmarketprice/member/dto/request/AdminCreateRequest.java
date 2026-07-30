package com.seoul.market.seoulmarketprice.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 관리자 계정 생성 요청 DTO.
 *
 * @param userId 관리자 로그인 아이디
 * @param password 암호화 전 평문 비밀번호
 * @param name 관리자 이름
 */
public record AdminCreateRequest(
        @NotBlank(message = "관리자 아이디는 필수입니다.")
        @Size(min = 4, max = 30, message = "관리자 아이디는 4자 이상 30자 이하여야 합니다.")
        @Pattern(
                regexp = "^[a-zA-Z0-9_]+$",
                message = "관리자 아이디는 영문, 숫자, 밑줄만 사용할 수 있습니다."
        )
        String userId,

        @NotBlank(message = "관리자 비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "관리자 비밀번호는 8자 이상 64자 이하여야 합니다.")
        String password,

        @NotBlank(message = "관리자 이름은 필수입니다.")
        @Size(max = 30, message = "관리자 이름은 30자 이하여야 합니다.")
        String name
) {
}
