package com.seoul.market.seoulmarketprice.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 사용자 아이디 중복 확인 요청 DTO.
 *
 * <p>
 * 회원가입 요청과 동일한 아이디 형식 검증 규칙을 적용하여,
 * 실제로 가입할 수 없는 형식의 아이디를 미리 조회하지 않도록 한다.
 * </p>
 *
 * @param userId 중복 여부를 확인할 사용자 아이디
 */
public record UserIdCheckRequest(
        /**
         * 영문, 숫자, 밑줄로 구성된 4자 이상 20자 이하의 아이디.
         */
        @NotBlank(message = "아이디는 필수입니다.")
        @Size(min = 4, max = 20, message = "아이디는 6자 이상 20자 이하여야 합니다.")
        @Pattern(
                regexp = "^[a-zA-Z0-9]+$",
                message = "아이디는 영문과 숫자만 사용할 수 있습니다."
        )
        String userId
) {
}
