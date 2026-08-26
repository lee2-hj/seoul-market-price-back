package com.seoul.market.seoulmarketprice.member.dto.request.member;

import jakarta.validation.constraints.AssertTrue;

/** 위치 기반 서비스 이용 동의를 기록하기 위한 요청 DTO. */
public record LocationConsentUpdateRequest(Boolean agreed) {

    @AssertTrue(message = "위치 서비스 동의가 필요합니다.")
    public boolean isAgreed() {
        return Boolean.TRUE.equals(agreed);
    }
}
