package com.seoul.market.seoulmarketprice.board.entity;

import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import com.seoul.market.seoulmarketprice.auth.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
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

/** 일반 게시글과 관리자 공지사항을 저장하는 게시판 엔티티다. */
@Entity
@Getter
@Table(name = "tb_board")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board {

    /** 게시글 고유 인덱스이다. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 일반 게시글 작성자의 tb_user 기본키이다. */
    @Column(name = "user_id")
    private Long userId;

    /** tb_board.user_id(FK)로 연결된 일반 사용자이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private Member user;

    /** 공지사항 작성자의 tb_member 기본키이다. */
    @Column(name = "member_id")
    private Long memberId;

    /** 공지사항 작성 관리자 정보를 조회하기 위한 연관 관계이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", insertable = false, updatable = false)
    private Admin member;

    /** 일반글과 공지사항을 구분하는 게시글 유형이다. */
    @Convert(converter = PostTypeConverter.class)
    @Column(name = "post_type", nullable = false)
    private PostType postType;

    /** 게시글 제목이다. */
    @Column(nullable = false, length = 200)
    private String title;

    /** HTML을 포함할 수 있는 게시글 본문이다. */
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    /** 첨부파일의 원본 파일명이다. */
//    경로에서 파일명만 가져오면 되므로 삭제(260807)
//    private String attachName;

    /** 첨부파일이 저장된 서버 또는 스토리지 경로이다. */
    @Column(name = "attach_path", length = 500)
    private String attachPath;

    /** 게시글 상세 조회 횟수이다. */
    @Column(name = "view_count", nullable = false)
    private int viewCount;

    /** 사용자에게 게시글을 노출할지 나타낸다. */
    @Column(name = "is_visible", nullable = false)
    private boolean visible;

    /** 게시글을 목록 상단에 고정할지 나타낸다. */
    @Column(name = "is_pinned", nullable = false)
    private boolean pinned;

    /** 게시글이 최초 저장된 시각이다. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 게시글이 마지막으로 수정된 시각이다. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 소프트 삭제된 시각이며 활성 게시글은 null이다. */
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

    /** 게시글 작성자의 로그인 아이디(tb_user.user_id)를 반환한다. */
    public String getWriterUserId() {
        return user == null ? null : user.getUserId();
    }

    /** 공지사항 작성 관리자의 로그인 아이디를 반환한다. */
    public String getWriterAdminId() {
        return member == null ? null : member.getAdminId();
    }

    /** 공지사항 작성 관리자의 이름을 반환한다. */
    public String getWriterAdminName() {
        return member == null ? null : member.getName();
    }

    /** 일반 게시글은 회원 이름, 공지사항은 관리자 이름을 반환한다. */
    public String getWriterName() {
        return postType == PostType.GENERAL
                ? (user == null ? null : user.getName())
                : getWriterAdminName();
    }

    /** 최초 저장 직전에 생성 시각을 기록한다. */
    @PrePersist
    private void prePersist() {
        this.createdAt = now();
    }

    /** 엔티티 변경 시 수정 시각이 없으면 현재 시각을 기록한다. */
    @PreUpdate
    private void preUpdate() {
        if (updatedAt == null) {
            updatedAt = now();
        }
    }

    /** DB 저장 정밀도에 맞춰 초 단위 현재 시각을 반환한다. */
    private static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
