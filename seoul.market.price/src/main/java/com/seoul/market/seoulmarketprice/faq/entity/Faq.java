package com.seoul.market.seoulmarketprice.faq.entity;

import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** 고객센터 자주 묻는 질문과 답변을 저장하는 엔티티이다. */
@Entity
@Getter
@Table(name = "tb_faq_board")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq {

    /** FAQ 고유 인덱스이다. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FAQ를 마지막으로 등록하거나 수정한 관리자 인덱스이다. */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 마지막으로 FAQ를 작성하거나 수정한 관리자이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", insertable = false, updatable = false)
    private Admin member;

    public String getWriterName() {
        return member == null ? null : member.getName();
    }

    /** 목록에 노출되는 질문 제목이다. */
    @Column(nullable = false, length = 300)
    private String question;

    /** 질문을 펼쳤을 때 노출되는 답변 내용이다. */
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String answer;

    /** 회원, 가격정보 등 FAQ 분류이다. */
    @Column(length = 50)
    private String category;

    /** FAQ 목록의 오름차순 노출 순서이다. */
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** 사용자 화면에 FAQ를 노출할지 나타낸다. */
    @Column(name = "is_visible", nullable = false)
    private boolean visible;

    /** FAQ 상세 조회 횟수이다. */
    @Column(name = "view_count", nullable = false)
    private int viewCount;

    /** FAQ가 최초 등록된 시각이다. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** FAQ가 마지막으로 수정된 시각이다. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 소프트 삭제된 시각이며 활성 FAQ는 null이다. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 관리자가 입력한 값으로 새로운 FAQ를 생성한다. */
    public static Faq create(
            Long memberId,
            String question,
            String answer,
            String category,
            int displayOrder,
            boolean visible
    ) {
        Faq faq = new Faq();
        faq.memberId = memberId;
        faq.question = question;
        faq.answer = answer;
        faq.category = category;
        faq.displayOrder = displayOrder;
        faq.visible = visible;
        faq.viewCount = 0;
        return faq;
    }

    /** 전달된 필드만 수정하고 작업한 관리자 인덱스를 기록한다. */
    public void update(
            Long memberId,
            String question,
            String answer,
            String category,
            boolean updateCategory,
            Integer displayOrder,
            Boolean visible
    ) {
        this.memberId = memberId;
        if (question != null) this.question = question;
        if (answer != null) this.answer = answer;
        if (updateCategory) this.category = category;
        if (displayOrder != null) this.displayOrder = displayOrder;
        if (visible != null) this.visible = visible;
        this.updatedAt = now();
    }

    /** FAQ를 실제 제거하지 않고 삭제 시각을 기록한다. */
    public void softDelete(Long memberId) {
        this.memberId = memberId;
        this.deletedAt = now();
        this.updatedAt = this.deletedAt;
    }

    /** 최초 저장 시 생성 시각을 기록한다. */
    @PrePersist
    private void prePersist() {
        this.createdAt = now();
    }

    /** 변경 시 수정 시각이 없으면 현재 시각을 기록한다. */
    @PreUpdate
    private void preUpdate() {
        if (updatedAt == null) updatedAt = now();
    }

    /** DB 저장 정밀도에 맞춰 초 단위 현재 시각을 반환한다. */
    private static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
