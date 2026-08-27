package com.seoul.market.seoulmarketprice.member.dto.request.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 일반 회원이 선택적으로 변경할 수 있는 계정·연락처·주소 정보 요청이다. */
public record MemberUpdateRequest(
        /** 변경할 새 비밀번호. 전달하지 않으면 기존 값을 유지한다. */
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        String password,

        /** 변경할 휴대전화 번호이며 010-1234-5678 형식만 허용한다. */
        @Pattern(
                regexp = "^010-\\d{4}-\\d{4}$",
                message = "휴대전화 번호는 010-1234-5678 형식이어야 합니다."
        )
        String phone,

        /** 휴대전화 번호 변경 시 프론트에서 완료한 PASS 본인인증 아이디. */
        String identityVerificationId,

        /** 변경할 이메일 주소. */
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email,

        /** 변경할 숫자 5자리 우편번호. */
        @Pattern(regexp = "^(\\d{5})?$", message = "우편번호는 숫자 5자리여야 합니다.")
        String zipcode,

        /** 변경할 기본 주소. */
        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        String address,

        /** 변경할 동·호수 등의 상세 주소. */
        @Size(max = 255, message = "상세 주소는 255자 이하여야 합니다.")
        String addressDetail,

        @JsonProperty("sgg_cd")
        @Size(max = 10)
        String sggCd
) {
    public MemberUpdateRequest(
            String password, String phone, String identityVerificationId,
            String email, String zipcode, String address, String addressDetail
    ) {
        this(password, phone, identityVerificationId, email, zipcode, address, addressDetail, null);
    }

    /** 휴대전화 번호를 변경하려면 해당 번호로 완료한 본인인증 결과가 필요하다. */
    @AssertTrue(message = "휴대전화 번호 변경 시 본인인증 아이디는 필수입니다.")
    public boolean isPhoneVerificationProvided() {
        return phone == null
                || (identityVerificationId != null && !identityVerificationId.isBlank());
    }

    /** 실제로 변경할 값이 하나 이상 전달되었는지 확인한다. */
    @AssertTrue(message = "변경할 회원 정보를 하나 이상 입력해야 합니다.")
    public boolean isAnyFieldProvided() {
        return password != null
                || phone != null
                || email != null
                || zipcode != null
                || address != null
                || addressDetail != null
                || sggCd != null;
    }
}
