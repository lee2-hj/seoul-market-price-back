package com.seoul.market.seoulmarketprice.menus.dto.request;

public record MenuCategoryRequest(
        String menuName,
        String menuCode,
        Integer page,
        Integer size
) {
}
