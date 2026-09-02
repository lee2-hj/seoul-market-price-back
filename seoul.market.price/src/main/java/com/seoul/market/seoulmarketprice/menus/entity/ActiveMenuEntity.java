package com.seoul.market.seoulmarketprice.menus.entity;

import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Getter
@Table(name = "tb_avtive_menu")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ActiveMenuEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", comment = "관리자 번호")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Admin admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", comment = "메뉴 카테고리 번호")
    @OnDelete(action = OnDeleteAction.CASCADE)
    MenuCategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", comment = "메뉴 번호")
    @OnDelete(action = OnDeleteAction.CASCADE)
    MenuEntity menu;

    @CreatedDate
    @Column(updatable = false, comment = "등록일")
    private LocalDateTime created_at;

    @LastModifiedDate
    @Column(comment = "수정일")
    private LocalDateTime  updated_at;

    /**
     * 등록 시 변경 시각을 초 단위까지만 기록한다.
     */
    @PrePersist
    private void prePersist() {
        this.created_at = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * 수정 시 변경 시각을 초 단위까지만 기록한다.
     */
    @PreUpdate
    private void preUpdate() {
        this.updated_at = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public ActiveMenuEntity(Admin admin, MenuCategoryEntity category, MenuEntity menu) {
        this.admin = admin;
        this.category = category;
        this.menu = menu;
    }

}
