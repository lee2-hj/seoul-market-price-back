package com.seoul.market.seoulmarketprice.menus.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MenuCategoryCreateRequest(
    @NotBlank(message = "메뉴 카테고리 코드는 필수입니다.")
    String menuCode,

    @NotBlank(message = "메뉴 카테고리명은 필수입니다.")
    String menuName
) {
}
