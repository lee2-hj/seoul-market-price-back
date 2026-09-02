package com.seoul.market.seoulmarketprice.menus.dto.request;

public record MenuRequest(
        String menuCategoryCode,
        String menuCode,
        String menuName,
        Integer page,
        Integer size
) {
}
