package com.seoul.market.seoulmarketprice.member.dto.request.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PASS 인증자와 입력한 회원 아이디를 확인하는 요청이다. */
public record PasswordResetVerifyRequest(
        @NotBlank(message = "본인인증 아이디는 필수입니다.")
        String identityVerificationId,

        @NotBlank(message = "아이디는 필수입니다.")
        @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하여야 합니다.")
        @Pattern(
                regexp = "^[a-zA-Z0-9_]+$",
                message = "아이디는 영문, 숫자, 밑줄만 사용할 수 있습니다."
        )
        String userId
) {
}
