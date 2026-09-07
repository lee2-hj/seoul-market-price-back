package com.seoul.market.seoulmarketprice.member.dto.request.member;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 위치기반 서비스 이용 동의 상태 변경 요청. 0은 비동의, 1은 동의다. */
public record LocationConsentUpdateRequest(
        @NotNull(message = "위치 서비스 동의 값은 필수입니다.")
        @Min(value = 0, message = "위치 서비스 동의 값은 0 또는 1이어야 합니다.")
        @Max(value = 1, message = "위치 서비스 동의 값은 0 또는 1이어야 합니다.")
        Byte agreed
) {
    public boolean isAgreed() {
        return agreed == 1;
    }
}
