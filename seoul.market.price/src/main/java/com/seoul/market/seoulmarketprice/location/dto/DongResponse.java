package com.seoul.market.seoulmarketprice.location.dto;

/** 프론트엔드의 행정동 선택 항목에 필요한 정보를 반환한다. */
public record DongResponse(
        /** 행정동의 표준 코드이다. */
        String dongCd,
        /** 사용자 화면에 표시하는 행정동 이름이다. */
        String dongNm
) {
}
