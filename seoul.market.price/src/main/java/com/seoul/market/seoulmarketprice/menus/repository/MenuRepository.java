package com.seoul.market.seoulmarketprice.menus.repository;

import com.seoul.market.seoulmarketprice.menus.entity.MenuEntity;
import com.seoul.market.seoulmarketprice.menus.repository.custom.MenuRepositoryCustom;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MenuRepository extends JpaRepository<MenuEntity,Long> , MenuRepositoryCustom {
    boolean existsByMenuCode(String menuCode);

    Optional<MenuEntity> findByCategory_MenuCodeAndMenuCode(String categoryMenuCode, String menuCode);
}
