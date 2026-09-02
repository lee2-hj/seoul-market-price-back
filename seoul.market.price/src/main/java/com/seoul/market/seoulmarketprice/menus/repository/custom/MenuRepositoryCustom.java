package com.seoul.market.seoulmarketprice.menus.repository.custom;

import com.seoul.market.seoulmarketprice.menus.entity.MenuEntity;

import java.util.Collection;
import java.util.List;

public interface MenuRepositoryCustom {
    List<MenuEntity> findmenuCategoryCodeOrmenuCodeOrmenuName(String menuCategoryCode, String menuCode, String menuName, long offset, int size);

    long countmenuCategoryCodeOrmenuCodeOrmenuName(String menuCategoryCode, String menuCode, String menuName);
}

