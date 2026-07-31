package com.seoul.market.seoulmarketprice.member.dto.request.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberUpdateRequest(
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        String password, //비밀번호

        @Pattern(
                regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",
                message = "휴대전화 번호 형식이 올바르지 않습니다."
        )
        String phone, //휴대폰번호

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email, //이메일

        @Pattern(regexp = "^(\\d{5})?$", message = "우편번호는 숫자 5자리여야 합니다.")
        String zipcode, //우편번호

        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        String address, //주소

        @Size(max = 255, message = "상세 주소는 255자 이하여야 합니다.")
        String addressDetail //상세주소
) {
}
