package com.seoul.market.seoulmarketprice.member.dto;

import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MemberCreateRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validRequestPassesValidation() {
        Set<ConstraintViolation<MemberCreateRequest>> violations =
                validator.validate(validRequest());

        assertThat(violations).isEmpty();
    }

    @Test
    void invalidRequestReportsFieldErrors() {
        MemberCreateRequest request = new MemberCreateRequest(
                "a!",
                "short",
                "",
                "123",
                "",
                null,
                "1234",
                "",
                "not-an-email",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Set<String> invalidFields = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(invalidFields).contains(
                "userId",
                "password",
                "name",
                "zipcode",
                "phone",
                "email"
        );
    }

    /** 하이픈 없는 휴대전화 번호가 회원가입 검증을 통과하지 않는지 확인한다. */
    @Test
    void phoneWithoutHyphensIsRejected() {
        Set<String> invalidFields = validator.validate(requestWithPhone("01012345678")).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(invalidFields).contains("phone");
    }

    private MemberCreateRequest validRequest() {
        return requestWithPhone("010-1234-5678");
    }

    /** 전화번호 값만 바꿔 회원가입 형식 검증을 비교할 수 있는 요청을 생성한다. */
    private MemberCreateRequest requestWithPhone(String phone) {
        return new MemberCreateRequest(
                "market_user",
                "password123!",
                "서울장터",
                "04524",
                "서울특별시 중구 세종대로 110",
                "1층",
                phone,
                "verification-id",
                "market@example.com",
                (byte) 1,
                (byte) 1,
                (byte) 1,
                "중구",
                "소공동",
                new BigDecimal("37.5642135"),
                new BigDecimal("126.9778292")
        );
    }
}
