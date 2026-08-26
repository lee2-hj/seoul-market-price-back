package com.seoul.market.seoulmarketprice.member.dto.request.admin;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminMemberUpdateRequest(
        @Pattern(regexp = "^(\\d{5})?$", message = "우편번호는 숫자 5자리여야 합니다.")
        String zipcode,
        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        String address,
        @Size(max = 255, message = "상세주소는 255자 이하여야 합니다.")
        String addressDetail,
        @Size(max = 50, message = "선호지역은 50자 이하여야 합니다.")
        String preferredRegion
) {
    public boolean hasChanges() {
        return zipcode != null || address != null || addressDetail != null || preferredRegion != null;
    }
}
