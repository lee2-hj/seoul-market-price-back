package com.seoul.market.seoulmarketprice.menus.repository;

import com.seoul.market.seoulmarketprice.menus.entity.ActiveMenuEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActiveMenuRepository extends JpaRepository<ActiveMenuEntity, Long> {

    @EntityGraph(attributePaths = {"category", "menu"})
    List<ActiveMenuEntity> findByAdminId(Long adminId);
}
