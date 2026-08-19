package com.seoul.market.seoulmarketprice.board.repository;

import com.seoul.market.seoulmarketprice.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/** 게시글의 저장과 기본 CRUD를 담당한다. */
public interface BoardRepository extends JpaRepository<Board, Long> {

    /** 삭제되지 않은 전체 게시글 수를 조회한다. */
    @Query("select count(b) from Board b where b.deletedAt is null")
    long countActivePosts();

    /** 지정 기간에 작성되었으며 삭제되지 않은 게시글 수를 조회한다. */
    @Query("""
            select count(b)
            from Board b
            where b.createdAt >= :from
              and b.createdAt < :to
              and b.deletedAt is null
            """)
    long countActivePostsCreatedBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
