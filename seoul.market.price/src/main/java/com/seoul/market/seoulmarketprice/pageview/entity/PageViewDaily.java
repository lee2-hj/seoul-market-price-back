package com.seoul.market.seoulmarketprice.pageview.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "tb_page_view_daily")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageViewDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "view_date", nullable = false, unique = true)
    private LocalDate viewDate;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static PageViewDaily create(LocalDate viewDate, LocalDateTime now) {
        PageViewDaily pageView = new PageViewDaily();
        pageView.viewDate = viewDate;
        pageView.viewCount = 0L;
        pageView.createdAt = now;
        pageView.updatedAt = now;
        return pageView;
    }

    public void increment(LocalDateTime now) {
        viewCount++;
        updatedAt = now;
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    private void preUpdate() {
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }
}
