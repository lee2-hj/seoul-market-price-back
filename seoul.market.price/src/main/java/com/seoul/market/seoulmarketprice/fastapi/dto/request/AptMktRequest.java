package com.seoul.market.seoulmarketprice.fastapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AptMktRequest(
        @NotBlank(message = "자치구 코드는 필수입니다.")
        String guCode,

        @NotBlank(message = "법정동 코드는 필수입니다.")
        String dongCode,

        @NotBlank(message = "아파트명은 필수입니다.")
        String aptName,

        @NotBlank(message = "지번은 필수입니다.")
        String mno,

        @NotBlank(message = "부번은 필수입니다.")
        String sno

) {
        public AptMktRequest{
                dongCode = DongCodeSupport.normalize(dongCode);
        }
}
