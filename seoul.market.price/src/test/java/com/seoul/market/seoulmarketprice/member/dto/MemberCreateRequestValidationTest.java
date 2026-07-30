package com.seoul.market.seoulmarketprice.member.dto;

import com.seoul.market.seoulmarketprice.member.dto.request.MemberCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

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
                "not-an-email"
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

    private MemberCreateRequest validRequest() {
        return new MemberCreateRequest(
                "market_user",
                "password123!",
                "서울장터",
                "04524",
                "서울특별시 중구 세종대로 110",
                "1층",
                "010-1234-5678",
                "market@example.com"
        );
    }
}
