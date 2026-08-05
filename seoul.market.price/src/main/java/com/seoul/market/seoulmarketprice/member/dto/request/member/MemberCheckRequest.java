package com.seoul.market.seoulmarketprice.member.dto.request.member;

import jakarta.validation.constraints.NotBlank;

public record MemberCheckRequest(
        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotBlank(message = "휴대전화 번호는 필수입니다.")
        String phone
) {
}
