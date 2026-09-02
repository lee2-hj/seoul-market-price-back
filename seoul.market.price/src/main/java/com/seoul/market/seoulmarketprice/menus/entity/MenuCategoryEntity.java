package com.seoul.market.seoulmarketprice.menus.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Getter
@Table(name = "tb_menu_category")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MenuCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "메뉴 카테고리 고유 인덱스")
    private Long id;

    @Column(name = "menu_code",comment = "메뉴 카테고리 코드", nullable = false, unique = true)
    private String menuCode;

    @Column(name = "menu_name",comment = "메뉴 카테고리명", nullable = false)
    private String menuName;

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

    //메뉴 등록 생성자
    public MenuCategoryEntity( String menuCode, String menuName) {
        this.menuCode = menuCode;
        this.menuName = menuName;
    }

    //메뉴 카테고리 업데이트 생성자
    public void updateMenuCategory(String menuCode, String menuName){
        this.menuCode = menuCode;
        this.menuName = menuName;
    }
}
