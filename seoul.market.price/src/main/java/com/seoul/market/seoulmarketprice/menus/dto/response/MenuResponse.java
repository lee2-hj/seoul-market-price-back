package com.seoul.market.seoulmarketprice.menus.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record MenuResponse(
        List<Menus> menus,
        long totalCount,
        int page,
        int size,
        int totalPages
) {
    public record Menus(Long id,
                        String menuCategoryCode,
                        String menuCategoryName,
                        String menuCode,
                        String menuName,
                        LocalDateTime createAt,
                        LocalDateTime updateAt) {}

    public static MenuResponse of(List<MenuResponse.Menus> menus, long totalCount, int page, int size) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalCount / size);
        return new MenuResponse(menus, totalCount, page, size, totalPages);
    }
}
