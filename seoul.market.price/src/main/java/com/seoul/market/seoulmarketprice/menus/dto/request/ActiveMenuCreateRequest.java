package com.seoul.market.seoulmarketprice.menus.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 관리자에게 새로 부여할 활성 메뉴 목록 요청 DTO이다.
 */
public record ActiveMenuCreateRequest(
        List<ActiveMenuItem> actives
) {
    public record ActiveMenuItem(

            @NotBlank(message = "메뉴 카테고리 코드를 입력하세요")
            String categoryCode,

            @NotBlank(message = "메뉴 코드를 입력하세요")
            String menuCode
    ) {
    }
}
