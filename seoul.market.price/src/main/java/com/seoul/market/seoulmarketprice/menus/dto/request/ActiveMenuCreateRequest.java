package com.seoul.market.seoulmarketprice.menus.dto.request;

import java.util.List;

/**
 * 관리자에게 새로 부여할 활성 메뉴 목록 요청 DTO이다.
 */
public record ActiveMenuCreateRequest(
        List<ActiveMenuItem> actives
) {
    public record ActiveMenuItem(
            String categoryCode,
            String menuCode
    ) {
    }
}
