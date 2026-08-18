package com.seoul.market.seoulmarketprice.fastapi.dto.request;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** DB에 저장된 10자리(자치구 5자리 + 법정동 5자리) 법정동 코드에서 뒤 5자리만 추출한다. */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
final class DongCodeSupport {

    private static final int FULL_LENGTH = 10;
    private static final int DONG_LENGTH = 5;

    static String normalize(String dongCode) {
        if (dongCode != null && dongCode.length() == FULL_LENGTH) {
            return dongCode.substring(FULL_LENGTH - DONG_LENGTH);
        }
        return dongCode;
    }
}
