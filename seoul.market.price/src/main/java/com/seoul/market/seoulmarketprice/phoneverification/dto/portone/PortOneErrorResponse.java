package com.seoul.market.seoulmarketprice.phoneverification.dto.portone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 포트원 V2 API가 4xx 오류 시 내려주는 공통 오류 응답이다.
 *
 * <p>
 * 예: {@code {"type":"IDENTITY_VERIFICATION_NOT_FOUND","message":"..."}}
 * </p>
 *
 * @param type    오류 종류
 * @param message 오류 메시지
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneErrorResponse(
        String type,
        String message
) {
}
