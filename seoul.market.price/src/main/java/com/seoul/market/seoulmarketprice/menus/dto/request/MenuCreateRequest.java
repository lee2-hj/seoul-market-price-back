package com.seoul.market.seoulmarketprice.menus.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MenuCreateRequest(
        @NotBlank(message = "메뉴 카테고리 id를 입력해주세요")
        Long menuCategoryId,

        @NotBlank(message = "메뉴코드를 입력하세요")
        String menuCode,

        @NotBlank(message = "메뉴명을 입력하세요")
        String menuName,

        String url
) {
}
