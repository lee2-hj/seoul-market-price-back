package com.seoul.market.seoulmarketprice.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** 일반 게시글과 관리자 공지사항을 저장하는 게시판 엔티티다. */
@Entity
@Getter
@Table(name = "tb_board")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "member_id")
    private Long memberId;

    @Convert(converter = PostTypeConverter.class)
    @Column(name = "post_type", nullable = false)
    private PostType postType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "attach_name", length = 255)
    private String attachName;

    @Column(name = "attach_path", length = 500)
    private String attachPath;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "is_visible", nullable = false)
    private boolean visible;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 사용자 작성자만 연결된 일반 게시글을 생성한다. */
    public static Board createGeneral(
            Long userId,
            String title,
            String content
    ) {
        Board board = new Board();
        board.userId = userId;
        board.memberId = null;
        board.postType = PostType.GENERAL;
        board.title = title;
        board.content = content;
        board.visible = true;
        board.pinned = false;
        board.viewCount = 0;
        return board;
    }

    /** 관리자 작성자만 연결된 공지사항을 생성한다. */
    public static Board createNotice(
            Long memberId,
            String title,
            String content,
            boolean visible,
            boolean pinned
    ) {
        Board board = new Board();
        board.userId = null;
        board.memberId = memberId;
        board.postType = PostType.NOTICE;
        board.title = title;
        board.content = content;
        board.visible = visible;
        board.pinned = pinned;
        board.viewCount = 0;
        return board;
    }

    /** 일반 사용자가 변경할 수 있는 제목과 본문만 수정한다. */
    public void updateGeneral(String title, String content) {
        this.title = title;
        this.content = content;
        this.updatedAt = now();
    }

    /** 관리자가 전달한 게시글 필드만 선택적으로 수정한다. */
    public void updateByAdmin(
            String title,
            String content,
            Boolean visible,
            Boolean pinned
    ) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (visible != null) {
            this.visible = visible;
        }
        if (pinned != null) {
            this.pinned = pinned;
        }
        this.updatedAt = now();
    }

    /** 게시글을 실제로 제거하지 않고 삭제 시각을 기록한다. */
    public void softDelete() {
        this.deletedAt = now();
        this.updatedAt = this.deletedAt;
    }

    /** 일반 게시글의 작성자가 전달된 사용자와 같은지 확인한다. */
    public boolean isOwnedBy(Long userId) {
        return postType == PostType.GENERAL
                && this.userId != null
                && this.userId.equals(userId);
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
