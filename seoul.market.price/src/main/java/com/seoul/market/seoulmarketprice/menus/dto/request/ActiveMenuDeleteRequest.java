package com.seoul.market.seoulmarketprice.menus.dto.request;

import java.util.List;

/**
 * 관리자에게서 해제할 활성 메뉴 고유번호 목록 요청 DTO이다.
 */
public record ActiveMenuDeleteRequest(
        List<Long> activeMenuIds
) {
}
