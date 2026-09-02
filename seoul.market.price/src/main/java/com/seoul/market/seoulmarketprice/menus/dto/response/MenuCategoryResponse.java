package com.seoul.market.seoulmarketprice.menus.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record MenuCategoryResponse(
        List<MenuInfo> menus,
        long totalCount,
        int page,
        int size,
        int totalPages
) {
    public record MenuInfo(Long id, String menuCode, String menuName, LocalDateTime createAt, LocalDateTime updateAt) {}

    public static MenuCategoryResponse of(List<MenuInfo> menus, long totalCount, int page, int size) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalCount / size);
        return new MenuCategoryResponse(menus, totalCount, page, size, totalPages);
    }

}
