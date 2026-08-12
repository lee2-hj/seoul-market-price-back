package com.seoul.market.seoulmarketprice.report.repository;

import com.seoul.market.seoulmarketprice.report.entity.Report;
import com.seoul.market.seoulmarketprice.report.entity.ReportCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/** 신고 저장과 활성 신고 조회를 담당하는 저장소이다. */
public interface ReportRepository extends JpaRepository<Report, Long> {
    /** 삭제되지 않은 신고를 작성자·답변 관리자 정보와 함께 조회한다. */
    @Query("""
            SELECT r FROM Report r JOIN FETCH r.user u LEFT JOIN FETCH r.admin a
            WHERE r.id = :id AND r.deletedAt IS NULL
            """)
    Optional<Report> findActiveById(@Param("id") Long id);

    /** 유형과 검색어를 적용해 삭제되지 않은 신고 목록을 페이지로 조회한다. */
    @Query(value = """
            SELECT r FROM Report r JOIN r.user u
            WHERE r.deletedAt IS NULL
              AND (:category IS NULL OR r.category = :category)
              AND (:keyword IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(r.targetProperty) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """, countQuery = """
            SELECT COUNT(r) FROM Report r
            WHERE r.deletedAt IS NULL
              AND (:category IS NULL OR r.category = :category)
              AND (:keyword IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(r.targetProperty) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Report> findPublicPage(@Param("category") ReportCategory category,
                                @Param("keyword") String keyword, Pageable pageable);
}
