package com.seoul.market.seoulmarketprice.member.dto.request.member;

import jakarta.validation.constraints.NotBlank;

/** 일반 회원 탈퇴 시 현재 비밀번호를 다시 확인한다. */
public record MemberWithdrawalRequest(
        /** 탈퇴 권한을 재확인하기 위한 현재 평문 비밀번호. */
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String password
) {
}
