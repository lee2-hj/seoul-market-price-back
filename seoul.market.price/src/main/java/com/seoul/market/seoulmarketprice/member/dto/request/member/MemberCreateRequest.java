package com.seoul.market.seoulmarketprice.member.dto.request.member;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.AssertTrue;

import java.math.BigDecimal;

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
         * 이 DTO는 일반 회원가입 전용이며(카카오 로그인은 별도 경로를 사용),
         * 일반 회원가입 시 비밀번호는 필수 입력이다.
         */
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 16자 이하여야 합니다.")
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
        @Pattern(regexp = "^(\\d{5})?$", message = "우편번호는 숫자 5자리여야 합니다.")
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
         * 010-1234-5678 형식만 허용한다.
         */
        @NotBlank(message = "휴대전화 번호는 필수입니다.")
        @Pattern(
                regexp = "^010-\\d{4}-\\d{4}$",
                message = "휴대전화 번호는 010-1234-5678 형식이어야 합니다."
        )
        String phone,

        /** 서버가 PASS 인증 결과와 CI를 다시 조회할 PortOne 본인인증 식별자. */
        @NotBlank(message = "본인인증 아이디는 필수입니다.")
        String identityVerificationId,

        /**
         * 이메일 형식의 사용자 연락처.
         */
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email,

        @NotNull(message = "이용약관유무는 필수입니다.")
        Byte is_terms_agreed,

        @NotNull(message = "위치기반 서비스 이용약관 동의 여부는 필수입니다.")
        Byte is_location_agreed,

        @NotNull(message = "개인정보 수집 및 이용동의 여부는 필수입니다.")
        Byte is_privacy_agreed,

        /** 사용자가 선호하는 서울시 자치구. */
        @Size(max = 50, message = "선호 자치구는 50자 이하여야 합니다.")
        String myGu,

        /** 사용자가 선호하는 행정동. 값이 있으면 자치구도 필수이다. */
        @Size(max = 50, message = "선호 행정동은 50자 이하여야 합니다.")
        String myDong,

        /** 선호 위치의 위도. 경도와 함께 전달해야 한다. */
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        BigDecimal latitude,

        /** 선호 위치의 경도. 위도와 함께 전달해야 한다. */
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        BigDecimal longitude
) {
        /** 좌표 한쪽만 저장되어 불완전한 위치가 생기는 것을 방지한다. */
        @AssertTrue(message = "위도와 경도는 함께 입력해야 합니다.")
        public boolean isCoordinatePairValid() {
                return (latitude == null) == (longitude == null);
        }

        /** 행정동만 존재하고 상위 자치구가 없는 지역 조합을 방지한다. */
        @AssertTrue(message = "선호 행정동을 입력하려면 선호 자치구가 필요합니다.")
        public boolean isPreferredAreaValid() {
                return myDong == null || myDong.isBlank()
                        || (myGu != null && !myGu.isBlank());
        }

        /** 검증된 요청과 서버가 확인한 CI를 이용해 일반 회원 엔티티를 생성한다. */
        public Member toEntity(String encodedPassword, String verifiedCi) {
                Member member = Member.createLocalMember(
                        this.userId,
                        encodedPassword,
                        this.name,
                        this.zipcode,
                        this.address,
                        this.addressDetail,
                        this.phone,
                        this.email,
                        this.is_terms_agreed,
                        this.is_location_agreed,
                        this.is_privacy_agreed,
                        this.myGu,
                        this.myDong,
                        this.latitude,
                        this.longitude
                );
                member.registerCi(verifiedCi);
                return member;
        }
}
