package com.seoul.market.seoulmarketprice.menus.repository;

import com.seoul.market.seoulmarketprice.menus.entity.MenuCategoryEntity;
import com.seoul.market.seoulmarketprice.menus.repository.custom.MenuCategoryRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuCategoryRepository extends JpaRepository<MenuCategoryEntity, Long>, MenuCategoryRepositoryCustom {}
