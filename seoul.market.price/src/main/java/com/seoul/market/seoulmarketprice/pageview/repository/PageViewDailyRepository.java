package com.seoul.market.seoulmarketprice.pageview.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class PageViewDailyRepository {

    private final JdbcTemplate jdbcTemplate;

    public void increment(LocalDate viewDate, LocalDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO tb_page_view_daily (view_date, view_count, created_at, updated_at)
                VALUES (?, 1, ?, ?)
                ON DUPLICATE KEY UPDATE
                    view_count = view_count + 1,
                    updated_at = VALUES(updated_at)
                """, viewDate, now, now);
    }

    public long countByDate(LocalDate viewDate) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT view_count FROM tb_page_view_daily WHERE view_date = ?",
                Long.class, viewDate);
        return count == null ? 0L : count;
    }
}
