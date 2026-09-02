package com.seoul.market.seoulmarketprice.menus.repository;

import com.seoul.market.seoulmarketprice.menus.entity.MenuEntity;
import com.seoul.market.seoulmarketprice.menus.repository.custom.MenuRepositoryCustom;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<MenuEntity,Long> , MenuRepositoryCustom {
    boolean existsByMenuCode(String menuCode);

    Optional<MenuEntity> findByCategory_MenuCodeAndMenuCode(String categoryMenuCode, String menuCode);

    /** MASTER 전용 전체 메뉴 카탈로그 조회용으로, 카테고리를 즉시 로딩(fetch join)한다. */
    @EntityGraph(attributePaths = "category")
    @Query("SELECT m FROM MenuEntity m")
    List<MenuEntity> findAllWithCategory();
}
