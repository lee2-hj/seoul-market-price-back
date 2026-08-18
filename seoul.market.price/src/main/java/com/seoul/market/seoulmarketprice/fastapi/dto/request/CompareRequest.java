package com.seoul.market.seoulmarketprice.fastapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CompareRequest(
        @NotBlank(message = "지역1 자치구 코드는 필수입니다.")
        String guCode1,
        @NotBlank(message = "지역1 법정동 코드는 필수입니다.")
        String dongCode1,
        @NotBlank(message = "지역2 자치구 코드는 필수입니다.")
        String guCode2,
        @NotBlank(message = "지역2 법정동 코드는 필수입니다.")
        String dongCode2
) {
    public CompareRequest {
        dongCode1 = DongCodeSupport.normalize(dongCode1);
        dongCode2 = DongCodeSupport.normalize(dongCode2);
    }
}
