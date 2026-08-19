package com.seoul.market.seoulmarketprice.qna.entity;

import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import com.seoul.market.seoulmarketprice.auth.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** 고객 질문과 관리자 답변을 함께 저장하는 Q&A 게시글 엔티티이다. */
@Entity
@Getter
@Table(name = "tb_qna_board")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaBoard {
    /** Q&A 게시글 고유 식별자이다. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 질문을 작성한 일반 사용자 식별자이다. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 작성자 표시 정보를 조회하기 위한 일반 사용자 연관 관계이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private Member user;

    /** 답변을 작성한 관리자 식별자이다. */
    @Column(name = "answer_member_id")
    private Long answerMemberId;

    /** 답변 관리자 표시 정보를 조회하기 위한 연관 관계이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_member_id", insertable = false, updatable = false)
    private Admin answerMember;

    /** 질문 제목이다. */
    @Column(nullable = false, length = 200)
    private String title;

    /** 웹 에디터 HTML을 포함할 수 있는 질문 본문이다. */
    @Column(name = "question_content", nullable = false, columnDefinition = "LONGTEXT")
    private String questionContent;

    /** 관리자가 등록한 답변 본문이다. */
    @Column(name = "answer_content", columnDefinition = "LONGTEXT")
    private String answerContent;

    /** 답변대기 또는 답변완료 상태이다. */
    @Convert(converter = AnswerStatusConverter.class)
    @Column(name = "answer_status", nullable = false)
    private AnswerStatus answerStatus;

    /** 상세 화면 조회 횟수이다. */
    @Column(name = "view_count", nullable = false)
    private int viewCount;

    /** 비로그인 사용자에게 질문을 공개할지 나타낸다. */
    @Column(name = "is_public", nullable = false)
    private boolean publicQuestion;

    /** 질문이 최초 등록된 시각이다. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 질문 또는 답변이 마지막으로 변경된 시각이다. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 관리자 답변이 마지막으로 등록된 시각이다. */
    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    /** 소프트 삭제 시각이며 활성 게시글은 null이다. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 일반 사용자가 작성하는 신규 Q&A 게시글을 생성한다. */
    public static QnaBoard create(Long userId, String title, String questionContent,
                                  boolean publicQuestion) {
        QnaBoard qna = new QnaBoard();
        qna.userId = userId;
        qna.title = title;
        qna.questionContent = questionContent;
        qna.publicQuestion = publicQuestion;
        qna.answerStatus = AnswerStatus.WAITING;
        qna.viewCount = 0;
        return qna;
    }

    /** 작성자가 전달한 질문 필드와 첨부파일 메타데이터를 선택적으로 변경한다. */
    public void updateQuestion(String title, String questionContent, Boolean publicQuestion) {
        if (title != null) this.title = title;
        if (questionContent != null) this.questionContent = questionContent;
        if (publicQuestion != null) this.publicQuestion = publicQuestion;
        this.updatedAt = now();
    }

    /** 관리자 답변을 저장하고 답변완료 상태와 답변 시각을 함께 기록한다. */
    public void answer(Long adminId, String content) {
        this.answerMemberId = adminId;
        this.answerContent = content;
        this.answerStatus = AnswerStatus.COMPLETED;
        this.answeredAt = now();
        this.updatedAt = this.answeredAt;
    }

    /** 기존 관리자 답변 정보를 제거하고 답변대기 상태로 되돌린다. */
    public void removeAnswer() {
        this.answerMemberId = null;
        this.answerContent = null;
        this.answerStatus = AnswerStatus.WAITING;
        this.answeredAt = null;
        this.updatedAt = now();
    }

    /** 질문의 공개 여부를 변경한다. */
    public void changeVisibility(boolean publicQuestion) {
        this.publicQuestion = publicQuestion;
        this.updatedAt = now();
    }

    /** 게시글을 물리적으로 제거하지 않고 삭제 시각을 기록한다. */
    public void softDelete() {
        this.deletedAt = now();
        this.updatedAt = this.deletedAt;
    }

    /** 전달된 사용자 식별자가 질문 작성자와 일치하는지 확인한다. */
    public boolean isOwnedBy(Long userId) {
        return userId != null && this.userId.equals(userId);
    }

    /** 연관된 작성자의 로그인 아이디를 반환한다. */
    public String getWriterLoginId() {
        return user == null ? null : user.getUserId();
    }

    /** 연관된 작성자의 이름을 반환한다. */
    public String getWriterName() {
        return user == null ? null : user.getName();
    }

    /** 연관된 답변 관리자의 이름을 반환한다. */
    public String getAnswerAdminName() {
        return answerMember == null ? null : answerMember.getName();
    }

    /** 최초 저장 직전에 기본 상태와 생성 시각을 보정한다. */
    @PrePersist
    private void prePersist() {
        if (answerStatus == null) answerStatus = AnswerStatus.WAITING;
        createdAt = now();
    }

    /** DB 정밀도에 맞춰 초 단위 현재 시각을 반환한다. */
    private static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
