package com.seoul.market.seoulmarketprice.member.dto.request.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 일회용 재설정 토큰으로 새 비밀번호를 적용하는 요청이다. */
public record PasswordResetCompleteRequest(
        @NotBlank(message = "비밀번호 재설정 토큰은 필수입니다.")
        String resetToken,

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        String newPassword,

        @NotBlank(message = "새 비밀번호 확인은 필수입니다.")
        String newPasswordConfirm
) {
}
