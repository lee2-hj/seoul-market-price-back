package com.seoul.market.seoulmarketprice.fastapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TopAndBottomRequest(

        @NotBlank(message = "자치구 코드는 필수입니다.")
        String guCode,

        @NotBlank(message = "법정동 코드는 필수입니다.")
        String dongCode
) {
    public TopAndBottomRequest {
        dongCode = DongCodeSupport.normalize(dongCode);
    }
}
