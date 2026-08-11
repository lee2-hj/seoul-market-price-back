package com.seoul.market.seoulmarketprice.member.dto;

import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberUpdateRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 회원정보 수정 요청의 휴대전화 번호 형식 검증을 확인한다. */
class MemberUpdateRequestValidationTest {
    /** Jakarta Validation 제약조건을 실행하는 검증기. */
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    /** 010-1234-5678 형식만 허용하고 하이픈 없는 번호는 거절하는지 확인한다. */
    @Test
    void acceptsOnlyHyphenated010PhoneFormat() {
        MemberUpdateRequest valid = new MemberUpdateRequest(
                null, "010-1234-5678", null, null, null, null
        );
        MemberUpdateRequest invalid = new MemberUpdateRequest(
                null, "01012345678", null, null, null, null
        );

        assertThat(validator.validate(valid)).isEmpty();
        assertThat(validator.validate(invalid))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("phone"));
    }
}
