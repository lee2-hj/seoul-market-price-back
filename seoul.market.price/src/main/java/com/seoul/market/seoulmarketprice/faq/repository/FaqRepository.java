package com.seoul.market.seoulmarketprice.faq.repository;

import com.seoul.market.seoulmarketprice.faq.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** FAQ 저장과 공개·관리자 조회를 담당한다. */
public interface FaqRepository extends JpaRepository<Faq, Long> {

    /** 공개 상태의 FAQ를 카테고리 조건과 노출 순서에 맞춰 조회한다. */
    @Query("""
            SELECT f FROM Faq f
            WHERE f.deletedAt IS NULL
              AND f.visible = true
              AND (:category IS NULL OR f.category = :category)
            ORDER BY f.displayOrder ASC, f.id ASC
            """)
    List<Faq> findPublicList(@Param("category") String category);

    /** 삭제되지 않고 공개된 FAQ 상세를 조회한다. */
    @Query("""
            SELECT f FROM Faq f
            WHERE f.id = :id
              AND f.deletedAt IS NULL
              AND f.visible = true
            """)
    Optional<Faq> findPublicById(@Param("id") Long id);

    /** 관리 화면에서 사용할 삭제되지 않은 FAQ 목록을 조회한다. */
    @Query("""
            SELECT f FROM Faq f
            WHERE f.deletedAt IS NULL
            ORDER BY f.displayOrder ASC, f.id ASC
            """)
    List<Faq> findAdminList();

    /** 노출 여부와 관계없이 삭제되지 않은 FAQ를 조회한다. */
    @Query("""
            SELECT f FROM Faq f
            WHERE f.id = :id
              AND f.deletedAt IS NULL
            """)
    Optional<Faq> findActiveById(@Param("id") Long id);

    /** 동시 조회 요청에서도 값이 유실되지 않도록 조회수를 직접 증가시킨다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Faq f
            SET f.viewCount = f.viewCount + 1
            WHERE f.id = :id
              AND f.deletedAt IS NULL
              AND f.visible = true
            """)
    int incrementViewCount(@Param("id") Long id);
}
