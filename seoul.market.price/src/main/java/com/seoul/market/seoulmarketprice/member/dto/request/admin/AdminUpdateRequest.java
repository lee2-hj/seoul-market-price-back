package com.seoul.market.seoulmarketprice.member.dto.request.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 관리자 정보 수정 요청 DTO이다. 전달하지 않은 항목은 유지하며 로그인 아이디는 수정할 수 없다. */
public record AdminUpdateRequest(
        @Size(min = 8, max = 64, message = "관리자 비밀번호는 8자 이상 64자 이하여야 합니다.")
        String password,
        @Size(min = 1, max = 30, message = "관리자 이름은 1자 이상 30자 이하여야 합니다.")
        String name,
        @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "휴대전화 번호 형식이 올바르지 않습니다.")
        String phone,
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
        String email
) {
}
