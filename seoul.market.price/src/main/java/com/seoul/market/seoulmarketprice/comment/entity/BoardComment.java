package com.seoul.market.seoulmarketprice.comment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** 여러 게시판에서 공통으로 사용할 댓글과 한 단계 대댓글을 저장한다. */
@Entity
@Getter
@Table(name = "tb_comment", indexes = {
        @Index(name = "idx_comment_post", columnList = "board_type, post_id, deleted_at, is_visible"),
        @Index(name = "idx_comment_parent", columnList = "parent_id, deleted_at"),
        @Index(name = "idx_comment_writer", columnList = "writer_type, writer_id, deleted_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type", nullable = false, length = 20)
    private BoardType boardType;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Enumerated(EnumType.STRING)
    @Column(name = "writer_type", nullable = false, length = 10)
    private WriterType writerType;

    @Column(name = "writer_id", nullable = false)
    private Long writerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private BoardComment parent;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_visible", nullable = false)
    private boolean visible;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "relation_note", length = 200)
    private String relationNote;

    /** 댓글 또는 대댓글을 생성하고 최초 노출 상태를 공개로 설정한다. */
    public static BoardComment create(
            BoardType boardType,
            Long postId,
            WriterType writerType,
            Long writerId,
            BoardComment parent,
            String content
    ) {
        BoardComment comment = new BoardComment();
        comment.boardType = boardType;
        comment.postId = postId;
        comment.writerType = writerType;
        comment.writerId = writerId;
        comment.parent = parent;
        comment.content = content;
        comment.visible = true;
        return comment;
    }

    /** 댓글 본문과 수정 시각을 변경한다. */
    public void update(String content) {
        this.content = content;
        this.updatedAt = now();
    }

    /** 관리자가 댓글의 공개 상태를 변경한다. */
    public void changeVisibility(boolean visible) {
        this.visible = visible;
        this.updatedAt = now();
    }

    /** 댓글을 실제로 제거하지 않고 삭제 시각을 기록한다. */
    public void softDelete() {
        this.deletedAt = now();
        this.updatedAt = this.deletedAt;
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public boolean isRoot() {
        return parent == null;
    }

    /** 작성자 유형과 ID가 모두 같은지 확인한다. */
    public boolean isOwnedBy(WriterType writerType, Long writerId) {
        return this.writerType == writerType
                && this.writerId.equals(writerId);
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = now();
    }

    @PreUpdate
    private void preUpdate() {
        if (updatedAt == null) {
            updatedAt = now();
        }
    }

    private static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
