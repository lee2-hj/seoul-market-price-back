package com.seoul.market.seoulmarketprice.board.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seoul.market.seoulmarketprice.board.dto.condition.BoardSearchCondition;
import com.seoul.market.seoulmarketprice.board.dto.condition.BoardSearchType;
import com.seoul.market.seoulmarketprice.board.entity.Board;
import com.seoul.market.seoulmarketprice.board.entity.QBoard;
import com.seoul.market.seoulmarketprice.auth.entity.QMember;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Locale;

/** QueryDSL로 공개 게시판 목록의 동적 검색과 페이징을 처리한다. */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardQueryRepository {
    /** 게시판 엔티티의 QueryDSL 메타 모델이다. */
    private static final QBoard board = QBoard.board;

    /** 타입 안전한 JPQL 쿼리를 생성하고 실행한다. */
    private final JPAQueryFactory queryFactory;

    /** 벌크 수정 후 영속성 컨텍스트를 초기화할 때 사용한다. */
    private final EntityManager entityManager;

    /** 노출 중인 활성 게시글을 검색 조건과 페이지 조건에 맞춰 조회한다. */
    public Page<Board> findPublicPage(BoardSearchCondition condition, Pageable pageable) {
        BooleanExpression search = searchCondition(condition.getSearchType(), condition.getKeyword());

        List<Board> content = queryFactory
                .selectFrom(board)
                .leftJoin(board.user).fetchJoin()
                .leftJoin(board.member).fetchJoin()
                .where(activeAndVisible(), search)
                .orderBy(board.pinned.desc(), board.createdAt.desc(), board.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(board.count())
                .from(board)
                .leftJoin(board.user)
                .leftJoin(board.member)
                .where(activeAndVisible(), search)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    /** 공개 상태이며 삭제되지 않은 게시글 상세를 조회한다. */
    public Optional<Board> findPublicById(Long id) {
        return Optional.ofNullable(queryFactory
                .selectFrom(board)
                .leftJoin(board.user).fetchJoin()
                .leftJoin(board.member).fetchJoin()
                .where(board.id.eq(id), activeAndVisible())
                .fetchOne());
    }

    /** 댓글 작업 전에 공개 게시글이 존재하는지 확인한다. */
    public boolean existsPublicById(Long id) {
        return queryFactory
                .selectOne()
                .from(board)
                .where(board.id.eq(id), activeAndVisible())
                .fetchFirst() != null;
    }

    /** 관리자 작업과 작성자 권한 확인을 위해 삭제되지 않은 게시글을 조회한다. */
    public Optional<Board> findActiveById(Long id) {
        return Optional.ofNullable(queryFactory
                .selectFrom(board)
                .leftJoin(board.user).fetchJoin()
                .leftJoin(board.member).fetchJoin()
                .where(board.id.eq(id), board.deletedAt.isNull())
                .fetchOne());
    }

    /** 공개 게시글의 조회수를 원자적으로 증가시킨다. */
    @Transactional
    public long incrementViewCount(Long id) {
        long updated = queryFactory
                .update(board)
                .set(board.viewCount, board.viewCount.add(1))
                .where(board.id.eq(id), activeAndVisible())
                .execute();
        entityManager.clear();
        return updated;
    }

    /** 삭제되지 않았고 사용자에게 노출되는 게시글 조건을 반환한다. */
    private BooleanExpression activeAndVisible() {
        return board.deletedAt.isNull().and(board.visible.isTrue());
    }

    /** 드롭다운 검색 타입에 맞는 제목·내용 또는 작성자 검색 조건을 반환한다. */
    private BooleanExpression searchCondition(BoardSearchType searchType, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String value = keyword.trim();
        BoardSearchType effectiveType = searchType == null
                ? BoardSearchType.TITLE_CONTENT
                : searchType;

        if (effectiveType == BoardSearchType.WRITER) {
            String normalized = value.toLowerCase(Locale.ROOT);
            List<Long> matchingUserIds = queryFactory.selectFrom(QMember.member)
                    .where(QMember.member.deleted_at.isNull())
                    .fetch()
                    .stream()
                    .filter(user -> user.getUserId().toLowerCase(Locale.ROOT).contains(normalized))
                    .map(user -> user.getId())
                    .toList();
            return board.userId.in(matchingUserIds)
                    .or(board.member.adminId.containsIgnoreCase(value))
                    .or(board.member.name.containsIgnoreCase(value));
        }

        return board.title.containsIgnoreCase(value)
                .or(board.content.containsIgnoreCase(value));
    }
}
