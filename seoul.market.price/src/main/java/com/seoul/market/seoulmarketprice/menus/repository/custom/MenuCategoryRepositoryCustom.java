package com.seoul.market.seoulmarketprice.menus.repository.custom;

import com.seoul.market.seoulmarketprice.menus.entity.MenuCategoryEntity;

import java.util.List;

public interface MenuCategoryRepositoryCustom {
    List<MenuCategoryEntity> findCodeOrName(String menuCode, String menuName, long offset, int size);
    long countMenuCodeOrMenuName(String menuCode, String menuName);
}
