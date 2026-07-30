package com.seoul.market.seoulmarketprice.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 일반 회원가입 요청 DTO.
 *
 * <p>
 * 클라이언트가 전달한 회원 정보를 검증한 뒤
 * Service 계층으로 전달한다. Member 엔티티를 요청 객체로 직접 사용하지 않는다.
 * </p>
 *
 * @param userId        로그인에 사용할 사용자 아이디
 * @param password      암호화 전 평문 비밀번호
 * @param name          사용자 이름
 * @param zipcode       배송지 우편번호
 * @param address       배송지 기본 주소
 * @param addressDetail 배송지 상세 주소
 * @param phone         휴대전화 번호
 * @param email         이메일 주소
 */
public record MemberCreateRequest(
        /**
         * 로그인 아이디.
         * 영문, 숫자, 밑줄을 조합하여 4자 이상 20자 이하로 입력한다.
         */
        @NotBlank(message = "아이디는 필수입니다.")
        @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하여야 합니다.")
        @Pattern(
                regexp = "^[a-zA-Z0-9_]+$",
                message = "아이디는 영문, 숫자, 밑줄만 사용할 수 있습니다."
        )
        String userId,

        /**
         * 로그인 비밀번호.
         * Service에서 BCrypt 방식으로 암호화한 후 저장한다.
         * 소셜 로그인때문에 password는 필수가 아님
         */
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        String password,

        /**
         * 사용자 실명 또는 서비스에서 사용할 이름.
         */
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 30, message = "이름은 30자 이하여야 합니다.")
        String name,

        /**
         * 우편번호는 선택
         */
        @Pattern(regexp = "^\\d{5}$", message = "우편번호는 숫자 5자리여야 합니다.")
        String zipcode,

        /**
         * 배송지 기본 주소.
         */
        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        String address,

        /**
         * 동·호수 등의 배송지 상세 주소.
         */
        @Size(max = 255, message = "상세 주소는 255자 이하여야 합니다.")
        String addressDetail,

        /**
         * 국내 휴대전화 번호.
         * 하이픈이 포함된 형식과 포함되지 않은 형식을 모두 허용한다.
         */
        @NotBlank(message = "휴대전화 번호는 필수입니다.")
        @Pattern(
                regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",
                message = "휴대전화 번호 형식이 올바르지 않습니다."
        )
        String phone,

        /**
         * 이메일 형식의 사용자 연락처.
         */
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email
) {
}
