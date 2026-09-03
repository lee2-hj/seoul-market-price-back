package com.seoul.market.seoulmarketprice.pageview.repository;

import com.seoul.market.seoulmarketprice.pageview.entity.PageViewDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface PageViewDailyRepository extends JpaRepository<PageViewDaily, Long> {
    Optional<PageViewDaily> findByViewDate(LocalDate viewDate);
}
