package com.seoul.market.seoulmarketprice.qna.repository;

import com.seoul.market.seoulmarketprice.qna.entity.QnaBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/** Q&A 게시글의 저장과 기본 CRUD를 담당한다. */
public interface QnaRepository extends JpaRepository<QnaBoard, Long> {

    /** 삭제되지 않은 전체 Q&A 수를 조회한다. */
    @Query("select count(q) from QnaBoard q where q.deletedAt is null")
    long countActivePosts();

    /** 지정 기간에 작성되었으며 삭제되지 않은 Q&A 수를 조회한다. */
    @Query("""
            select count(q)
            from QnaBoard q
            where q.createdAt >= :from
              and q.createdAt < :to
              and q.deletedAt is null
            """)
    long countActivePostsCreatedBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
