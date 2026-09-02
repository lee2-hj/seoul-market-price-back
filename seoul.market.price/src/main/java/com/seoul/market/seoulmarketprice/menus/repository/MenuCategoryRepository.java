package com.seoul.market.seoulmarketprice.menus.repository;

import com.seoul.market.seoulmarketprice.menus.entity.MenuCategoryEntity;
import com.seoul.market.seoulmarketprice.menus.repository.custom.MenuCategoryRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuCategoryRepository extends JpaRepository<MenuCategoryEntity, Long>, MenuCategoryRepositoryCustom {

    Optional<MenuCategoryEntity> findByMenuCode(String menuCode);
}
