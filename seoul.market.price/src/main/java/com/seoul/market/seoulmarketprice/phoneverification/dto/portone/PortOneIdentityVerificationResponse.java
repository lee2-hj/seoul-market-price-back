package com.seoul.market.seoulmarketprice.phoneverification.dto.portone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 포트원 V2 "본인인증 단건 조회" API의 원본 응답이다.
 *
 * <p>
 * 실제 응답은 {@code status}(READY/VERIFIED/FAILED)에 따라
 * 서로 다른 필드를 담은 세 가지 형태 중 하나로 내려온다.
 * 여기서는 세 형태에서 필요한 필드만 모아 하나의 record로
 * 유연하게 받는다. status에 따라 존재하지 않는 필드는 null이다.
 * </p>
 *
 * @param status          본인인증 상태 (READY, VERIFIED, FAILED)
 * @param id              고객사 본인인증 아이디
 * @param verifiedCustomer 인증 완료(VERIFIED)일 때만 채워지는 인증된 고객 정보
 * @param failure         인증 실패(FAILED)일 때만 채워지는 실패 정보
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneIdentityVerificationResponse(
        String status,
        String id,
        String verifiedAt,
        VerifiedCustomer verifiedCustomer,
        Failure failure
) {

    /**
     * 인증 완료 시 내려오는 고객 정보.
     *
     * @param name        이름
     * @param phoneNumber 전화번호 (숫자만, 하이픈 없음)
     * @param birthDate   생년월일 (yyyy-MM-dd)
     * @param gender      성별 (MALE, FEMALE, OTHER)
     * @param isForeigner 외국인 여부
     * @param ci          CI (개인 고유 식별키)
     * @param di          DI (사이트별 개인 고유 식별키)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VerifiedCustomer(
            String name,
            String phoneNumber,
            String birthDate,
            String gender,
            Boolean isForeigner,
            String ci,
            String di
    ) {
    }

    /**
     * 인증 실패 시 내려오는 실패 정보.
     *
     * @param reason     실패 사유
     * @param pgCode     PG사 실패 코드
     * @param pgMessage  PG사 실패 메시지
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Failure(
            String reason,
            String pgCode,
            String pgMessage
    ) {
    }
}
