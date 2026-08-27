package com.seoul.market.seoulmarketprice.fastapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AptCompareRequest(

        @NotBlank(message = "자치구 코드는 필수입니다.")
        String guCode,

        @NotBlank(message = "법정동 코드는 필수입니다.")
        String dongCode,

        String aptName,

        @NotBlank(message = "지번을 입력하세요")
        String mno,

        @NotBlank(message = "부번을 입력하세요")
        String sno,

        @NotBlank(message = "평 | 층수 중 하나를 선택 하세요.")
        String queryType,

        @NotBlank(message = "selectGroup1 값을 입력하세요")
        String selectGroup1,

        @NotBlank(message = "selectGroup2 값을 입력하세요")
        String selectGroup2
) {
        public AptCompareRequest {
                dongCode = DongCodeSupport.normalize(dongCode);
        }
}
