package com.seoul.market.seoulmarketprice.qna.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seoul.market.seoulmarketprice.qna.dto.condition.AdminQnaSearchCondition;
import com.seoul.market.seoulmarketprice.qna.dto.condition.QnaSearchCondition;
import com.seoul.market.seoulmarketprice.qna.entity.AnswerStatus;
import com.seoul.market.seoulmarketprice.qna.entity.QnaBoard;
import com.seoul.market.seoulmarketprice.qna.entity.QQnaBoard;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** QueryDSL로 Q&A의 화면별 동적 조회와 벌크 수정을 처리한다. */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaQueryRepository {
    /** Q&A 엔티티의 QueryDSL 메타 모델이다. */
    private static final QQnaBoard qna = QQnaBoard.qnaBoard;

    /** 타입 안전한 JPQL 쿼리를 생성하고 실행한다. */
    private final JPAQueryFactory queryFactory;

    /** 벌크 수정 후 영속성 컨텍스트를 초기화할 때 사용한다. */
    private final EntityManager entityManager;

    /** 공개된 활성 Q&A를 제목 키워드와 답변 상태로 검색한다. */
    public Page<QnaBoard> findPublicPage(QnaSearchCondition condition, Pageable pageable) {
        List<QnaBoard> content = queryFactory
                .selectFrom(qna)
                .leftJoin(qna.user).fetchJoin()
                .where(active(), publicOnly(), titleContains(condition.getKeyword()),
                        answerStatusEq(condition.getStatus()))
                .orderBy(qna.createdAt.desc(), qna.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(qna.count())
                .from(qna)
                .where(active(), publicOnly(), titleContains(condition.getKeyword()),
                        answerStatusEq(condition.getStatus()))
                .fetchOne();

        return page(content, pageable, total);
    }

    /** 로그인 사용자가 작성한 활성 Q&A를 검색한다. */
    public Page<QnaBoard> findMyPage(Long userId, QnaSearchCondition condition, Pageable pageable) {
        List<QnaBoard> content = queryFactory
                .selectFrom(qna)
                .leftJoin(qna.user).fetchJoin()
                .where(active(), qna.userId.eq(userId), titleContains(condition.getKeyword()),
                        answerStatusEq(condition.getStatus()))
                .orderBy(qna.createdAt.desc(), qna.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(qna.count())
                .from(qna)
                .where(active(), qna.userId.eq(userId), titleContains(condition.getKeyword()),
                        answerStatusEq(condition.getStatus()))
                .fetchOne();

        return page(content, pageable, total);
    }

    /** 공개 질문 또는 요청 사용자가 작성한 비공개 질문의 상세를 조회한다. */
    public Optional<QnaBoard> findAccessibleById(Long id, Long userId) {
        return Optional.ofNullable(queryFactory
                .selectFrom(qna)
                .leftJoin(qna.user).fetchJoin()
                .leftJoin(qna.answerMember).fetchJoin()
                .where(qna.id.eq(id), active(), accessibleBy(userId))
                .fetchOne());
    }

    /** 관리자 작업 또는 작성자 권한 확인을 위해 활성 질문을 조회한다. */
    public Optional<QnaBoard> findActiveById(Long id) {
        return Optional.ofNullable(queryFactory
                .selectFrom(qna)
                .leftJoin(qna.user).fetchJoin()
                .leftJoin(qna.answerMember).fetchJoin()
                .where(qna.id.eq(id), active())
                .fetchOne());
    }

    /** 백오피스의 복합 검색 조건으로 활성 Q&A 목록을 조회한다. */
    public Page<QnaBoard> findAdminPage(AdminQnaSearchCondition condition, Pageable pageable) {
        LocalDateTime from = condition.getFrom() == null
                ? null : condition.getFrom().atStartOfDay();
        LocalDateTime to = condition.getTo() == null
                ? null : condition.getTo().plusDays(1).atStartOfDay();
        List<QnaBoard> content = queryFactory
                .selectFrom(qna)
                .leftJoin(qna.user).fetchJoin()
                .leftJoin(qna.answerMember).fetchJoin()
                .where(active(), keywordContains(condition.getKeyword()),
                        answerStatusEq(condition.getStatus()),
                        publicQuestionEq(condition.getPublicQuestion()),
                        writerContains(condition.getWriter()),
                        createdAtGoe(from), createdAtLt(to))
                .orderBy(qna.createdAt.desc(), qna.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(qna.count())
                .from(qna)
                .leftJoin(qna.user)
                .where(active(), keywordContains(condition.getKeyword()),
                        answerStatusEq(condition.getStatus()),
                        publicQuestionEq(condition.getPublicQuestion()),
                        writerContains(condition.getWriter()),
                        createdAtGoe(from), createdAtLt(to))
                .fetchOne();

        return page(content, pageable, total);
    }

    /** 접근 가능한 상세 게시글의 조회수를 원자적으로 증가시킨다. */
    @Transactional
    public long incrementViewCount(Long id, Long userId) {
        long updated = queryFactory
                .update(qna)
                .set(qna.viewCount, qna.viewCount.add(1))
                .where(qna.id.eq(id), active(), accessibleBy(userId))
                .execute();

        entityManager.clear();
        return updated;
    }

    /** 삭제되지 않은 게시글 조건을 반환한다. */
    private BooleanExpression active() {
        return qna.deletedAt.isNull();
    }

    /** 공개 게시글 조건을 반환한다. */
    private BooleanExpression publicOnly() {
        return qna.publicQuestion.isTrue();
    }

    /** 공개 글 또는 본인 글에만 접근할 수 있는 조건을 구성한다. */
    private BooleanExpression accessibleBy(Long userId) {
        BooleanExpression accessible = publicOnly();
        return userId == null ? accessible : accessible.or(qna.userId.eq(userId));
    }

    /** 제목 키워드가 있을 때 대소문자를 구분하지 않는 검색 조건을 반환한다. */
    private BooleanExpression titleContains(String keyword) {
        return hasText(keyword) ? qna.title.containsIgnoreCase(keyword.trim()) : null;
    }

    /** 제목 또는 질문 본문을 대상으로 하는 관리자 키워드 조건을 반환한다. */
    private BooleanExpression keywordContains(String keyword) {
        if (!hasText(keyword)) return null;
        String value = keyword.trim();
        return qna.title.containsIgnoreCase(value)
                .or(qna.questionContent.containsIgnoreCase(value));
    }

    /** 답변 상태가 전달된 경우 일치 조건을 반환한다. */
    private BooleanExpression answerStatusEq(AnswerStatus status) {
        return status == null ? null : qna.answerStatus.eq(status);
    }

    /** 공개 여부가 전달된 경우 일치 조건을 반환한다. */
    private BooleanExpression publicQuestionEq(Boolean publicQuestion) {
        return publicQuestion == null ? null : qna.publicQuestion.eq(publicQuestion);
    }

    /** 작성자 로그인 아이디 검색 조건을 반환한다. */
    private BooleanExpression writerContains(String writer) {
        return hasText(writer) ? qna.user.userId.containsIgnoreCase(writer.trim()) : null;
    }

    /** 작성 시각의 검색 시작 조건을 반환한다. */
    private BooleanExpression createdAtGoe(LocalDateTime from) {
        return from == null ? null : qna.createdAt.goe(from);
    }

    /** 작성 시각의 검색 종료 미만 조건을 반환한다. */
    private BooleanExpression createdAtLt(LocalDateTime to) {
        return to == null ? null : qna.createdAt.lt(to);
    }

    /** 조회 결과와 전체 건수로 Spring Data 페이지 응답을 생성한다. */
    private Page<QnaBoard> page(List<QnaBoard> content, Pageable pageable, Long total) {
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    /** null과 공백을 제외한 검색 문자열인지 확인한다. */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
