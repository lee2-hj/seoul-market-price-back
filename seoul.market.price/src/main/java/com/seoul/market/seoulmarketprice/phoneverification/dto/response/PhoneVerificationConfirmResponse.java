package com.seoul.market.seoulmarketprice.phoneverification.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 휴대폰 PASS 본인인증 결과 확인 응답 DTO.
 *
 * <p>
 * 포트원 서버에서 실제로 확인한 인증 결과만 담는다.
 * 프론트엔드는 이 응답의 이름/전화번호를 회원가입 폼에
 * 채워 넣는 용도로 사용할 수 있다.
 * </p>
 *
 * @param verified    본인인증 성공 여부 (항상 true, 실패 시 예외로 처리한다)
 * @param name        인증된 이름
 * @param phoneNumber 인증된 전화번호 (숫자만, 하이픈 없음)
 * @param birthDate   인증된 생년월일 (yyyy-MM-dd)
 * @param gender      인증된 성별 (MALE, FEMALE, OTHER)
 */
public record PhoneVerificationConfirmResponse(
        boolean verified,
        String name,
        String phoneNumber,
        String birthDate,
        String gender,
        @JsonIgnore String verifiedAt,
        @JsonIgnore String ci
) {
    public PhoneVerificationConfirmResponse(
            boolean verified,
            String name,
            String phoneNumber,
            String birthDate,
            String gender
    ) {
        this(verified, name, phoneNumber, birthDate, gender, null, null);
    }
}
