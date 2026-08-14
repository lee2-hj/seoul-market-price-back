package com.seoul.market.seoulmarketprice.location.dto;

/** 프론트엔드의 자치구 선택 항목에 필요한 정보를 반환한다. */
public record SggResponse(
        /** 자치구의 표준 코드이다. */
        String sggCd,
        /** 사용자 화면에 표시하는 자치구 이름이다. */
        String sggNm
) {
}
