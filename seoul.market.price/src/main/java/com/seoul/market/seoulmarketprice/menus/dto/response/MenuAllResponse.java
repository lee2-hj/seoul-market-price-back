package com.seoul.market.seoulmarketprice.menus.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record MenuAllResponse(
        List<Menus> menus
) {
    public record Menus(Long id,
                        String menuCategoryCode,
                        String menuCategoryName,
                        String menuCode,
                        String menuName,
                        String url,
                        LocalDateTime createAt,
                        LocalDateTime updateAt) {}
}
