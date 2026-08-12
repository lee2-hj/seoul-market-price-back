package com.seoul.market.seoulmarketprice.report.entity;

import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import com.seoul.market.seoulmarketprice.auth.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** 회원이 등록한 신고와 관리자 처리 결과를 보관하는 엔티티이다. */
@Entity @Getter
@Table(name = "tb_report", indexes = {
        @Index(name = "idx_report_created", columnList = "deleted_at,created_at"),
        @Index(name = "idx_report_user", columnList = "user_id,deleted_at"),
        @Index(name = "idx_report_category_status", columnList = "category,status,deleted_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {
    /** 신고 고유 식별자. */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    /** 신고 작성 회원의 PK. */
    @Column(name = "user_id", nullable = false) private Long userId;
    /** 작성자 정보 조회를 위한 읽기 전용 연관관계. */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", insertable = false, updatable = false) private Member user;
    /** 신고 유형과 현재 처리 상태. */
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ReportCategory category;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ReportStatus status;
    @Column(name = "target_property", nullable = false, length = 200) private String targetProperty;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, columnDefinition = "LONGTEXT") private String content;
    @Column(name = "is_secret", nullable = false) private boolean secret;
    @Column(name = "admin_id") private Long adminId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "admin_id", insertable = false, updatable = false) private Admin admin;
    @Column(name = "admin_reply", columnDefinition = "LONGTEXT") private String adminReply;
    @Column(name = "replied_at") private LocalDateTime repliedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Column(name = "deleted_at") private LocalDateTime deletedAt;

    /** 검증된 회원 요청으로 접수 상태의 신고를 생성한다. */
    public static Report create(Long userId, ReportCategory category, String targetProperty,
                                String title, String content, boolean secret) {
        Report report = new Report();
        report.userId = userId; report.category = category; report.status = ReportStatus.RECEIVED;
        report.targetProperty = targetProperty; report.title = title; report.content = content; report.secret = secret;
        return report;
    }
    /** 요청 회원이 신고 작성자인지 PK로 확인한다. */
    public boolean isOwnedBy(Long memberId) { return memberId != null && memberId.equals(userId); }
    /** 관리자가 전달한 상태와 답변 중 존재하는 값만 반영한다. */
    public void updateByAdmin(Long adminId, ReportStatus status, String reply) {
        if (status != null) this.status = status;
        if (reply != null) { this.adminId = adminId; this.adminReply = reply; this.repliedAt = now(); }
    }
    /** 신고 행을 제거하지 않고 삭제 시각을 기록한다. */
    public void softDelete() { deletedAt = now(); }
    /** 최초 저장 시 생성 시각을 기록한다. */
    @PrePersist private void prePersist() { createdAt = now(); }
    /** 엔티티 변경 시 수정 시각을 기록한다. */
    @PreUpdate private void preUpdate() { updatedAt = now(); }
    /** DB 시간 정밀도에 맞춘 현재 시각을 반환한다. */
    private static LocalDateTime now() { return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS); }
}
    /** 신고 대상, 제목, 상세 내용과 비밀글 여부. */
    /** 마지막 답변을 등록한 관리자 PK와 읽기 전용 연관관계. */
    /** 관리자 답변과 답변 등록 시각. */
    /** 생성·수정·소프트 삭제 시각. */
