package com.seoul.market.seoulmarketprice.common.dto;

/**
 * 공통 에러 응답 DTO.
 *
 * @param code 에러 코드
 * @param message 에러 메시지
 */
public record ErrorResponse(
        String code,
        String message
) {
}