package com.seoul.market.seoulmarketprice.fastapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ListRequest(
        @NotBlank(message = "자치구 코드는 필수입니다.")
        String guCode
) {
}
