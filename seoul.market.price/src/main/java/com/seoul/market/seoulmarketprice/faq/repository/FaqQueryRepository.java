package com.seoul.market.seoulmarketprice.faq.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seoul.market.seoulmarketprice.faq.entity.Faq;
import com.seoul.market.seoulmarketprice.faq.entity.QFaq;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** QueryDSL로 FAQ 공개·관리자 조회와 조회수 증가를 처리한다. */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaqQueryRepository {
    /** FAQ 엔티티의 QueryDSL 메타 모델이다. */
    private static final QFaq faq = QFaq.faq;

    /** 타입 안전한 JPQL 쿼리를 생성하고 실행한다. */
    private final JPAQueryFactory queryFactory;

    /** 벌크 수정 후 영속성 컨텍스트를 초기화할 때 사용한다. */
    private final EntityManager entityManager;

    /** 공개 FAQ를 선택한 카테고리와 노출 순서에 맞춰 조회한다. */
    public List<Faq> findPublicList(String category) {
        return queryFactory
                .selectFrom(faq)
                .leftJoin(faq.member).fetchJoin()
                .where(active(), faq.visible.isTrue(), categoryEq(category))
                .orderBy(faq.displayOrder.asc(), faq.id.asc())
                .fetch();
    }

    /** 공개 상태이며 삭제되지 않은 FAQ 상세를 조회한다. */
    public Optional<Faq> findPublicById(Long id) {
        return Optional.ofNullable(queryFactory
                .selectFrom(faq)
                .leftJoin(faq.member).fetchJoin()
                .where(faq.id.eq(id), active(), faq.visible.isTrue())
                .fetchOne());
    }

    /** 삭제되지 않은 모든 FAQ를 관리자 노출 순서로 조회한다. */
    public List<Faq> findAdminList(String keyword) {
        return queryFactory
                .selectFrom(faq)
                .leftJoin(faq.member).fetchJoin()
                .where(active(), keywordContains(keyword))
                .orderBy(faq.displayOrder.asc(), faq.id.asc())
                .fetch();
    }

    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return faq.question.containsIgnoreCase(keyword).or(faq.answer.containsIgnoreCase(keyword));
    }

    /** 관리자 작업을 위해 삭제되지 않은 FAQ를 조회한다. */
    public Optional<Faq> findActiveById(Long id) {
        return Optional.ofNullable(queryFactory
                .selectFrom(faq)
                .leftJoin(faq.member).fetchJoin()
                .where(faq.id.eq(id), active())
                .fetchOne());
    }

    /** 공개 FAQ의 조회수를 원자적으로 증가시킨다. */
    @Transactional
    public long incrementViewCount(Long id) {
        long updated = queryFactory
                .update(faq)
                .set(faq.viewCount, faq.viewCount.add(1))
                .where(faq.id.eq(id), active(), faq.visible.isTrue())
                .execute();
        entityManager.clear();
        return updated;
    }

    /** 삭제되지 않은 FAQ 조건을 반환한다. */
    private BooleanExpression active() {
        return faq.deletedAt.isNull();
    }

    /** 카테고리가 전달된 경우에만 일치 조건을 반환한다. */
    private BooleanExpression categoryEq(String category) {
        return category == null ? null : faq.category.eq(category);
    }
}
