package com.seoul.market.seoulmarketprice.member.dto.request.member;

import jakarta.validation.constraints.NotBlank;

/** 아이디 찾기에 사용할 PASS 본인인증 식별자를 전달한다. */
public record MemberIdFindRequest(
        @NotBlank(message = "본인인증 아이디는 필수입니다.")
        String identityVerificationId
) {
}
