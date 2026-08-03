package com.seoul.market.seoulmarketprice.phoneverification.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 휴대폰 PASS 본인인증 결과 확인 요청 DTO.
 *
 * <p>
 * 프론트엔드에서 포트원 브라우저 SDK로 PASS 본인인증을 완료하면
 * 발급되는 identityVerificationId를 전달받는다.
 * </p>
 *
 * @param identityVerificationId 포트원 브라우저 SDK가 발급한 본인인증 아이디
 */
public record PhoneVerificationConfirmRequest(
        @NotBlank(message = "본인인증 아이디는 필수입니다.")
        String identityVerificationId
) {
}
