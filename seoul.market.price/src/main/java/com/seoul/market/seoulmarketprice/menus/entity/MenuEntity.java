package com.seoul.market.seoulmarketprice.menus.entity;

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
@Table(name = "tb_menu")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MenuEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", comment = "메뉴 카테고리 아이디")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MenuCategoryEntity category; //메뉴 카테고리 조인

    @Column(name = "menu_code", nullable = false, unique = true, comment = "메뉴코드")
    private String menuCode;

    @Column(name = "menu_name", nullable = false , comment = "메뉴명")
    private String menuName;

    @Column(comment = "이동 url")
    private String url;

    @Column(updatable = false, comment = "등록일")
    private LocalDateTime created_at;

    @Column(insertable = false, columnDefinition = "datetime", comment = "수정일")
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

    public MenuEntity(MenuCategoryEntity category, String menuCode, String menuName, String url) {
        this.category = category;
        this.menuCode = menuCode;
        this.menuName = menuName;
        this.url = url;
    }

    public void updateMenu(MenuCategoryEntity category, String menuCode, String menuName, String url){
        this.category = category;
        this.menuCode = menuCode;
        this.menuName = menuName;
        this.url = url;
    }
}
