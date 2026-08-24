package com.seoul.market.seoulmarketprice.fastapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegionAptCompareRequest(
        @NotBlank(message = "자치구 코드1은 필수입니다.")
        String guCode1,

        @NotBlank(message = "법정동 코드1은 필수입니다.")
        String dongCode1,

        @NotBlank(message = "아파트명1은 필수입니다.")
        String aptName1,

        @NotBlank(message = "지번1은 필수입니다.")
        String mno1,

        @NotBlank(message = "부번1은 필수입니다.")
        String sno1,

        @NotBlank(message = "자치구 코드2는 필수입니다.")
        String guCode2,

        @NotBlank(message = "법정동 코드2는 필수입니다.")
        String dongCode2,

        @NotBlank(message = "아파트명2는 필수입니다.")
        String aptName2,

        @NotBlank(message = "지번2는 필수입니다.")
        String mno2,

        @NotBlank(message = "부번2는 필수입니다.")
        String sno2
) {
    public RegionAptCompareRequest {
        dongCode1 = DongCodeSupport.normalize(dongCode1);
        dongCode2 = DongCodeSupport.normalize(dongCode2);
    }
}
