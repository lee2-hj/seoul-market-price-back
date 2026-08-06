package com.seoul.market.seoulmarketprice.board.repository;

import com.seoul.market.seoulmarketprice.board.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/** 게시글 저장과 공개·활성 게시글 조회를 담당한다. */
public interface BoardRepository extends JpaRepository<Board, Long> {
    /** 공개 상태이며 삭제되지 않은 게시글을 제목 검색 조건으로 조회한다. */
    @EntityGraph(attributePaths = "user")
    @Query("""
            SELECT b
            FROM Board b
            WHERE b.deletedAt IS NULL
              AND b.visible = true
              AND (
                    :keyword IS NULL
                    OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<Board> findPublicPage(@Param("keyword") String keyword, Pageable pageable);
    /** 공개 상태이며 삭제되지 않은 게시글 상세를 조회한다. */
    @EntityGraph(attributePaths = "user")
    @Query("""
            SELECT b FROM Board b
            WHERE b.id = :id
              AND b.deletedAt IS NULL
              AND b.visible = true
            """)
    Optional<Board> findPublicById(@Param("id") Long id);
    /** 댓글 조회와 작성 전에 공개 게시글 존재 여부를 확인한다. */
    @Query("""
            SELECT (COUNT(b) > 0) FROM Board b
            WHERE b.id = :id
              AND b.deletedAt IS NULL
              AND b.visible = true
            """)
    boolean existsPublicById(@Param("id") Long id);
    /** 관리자 작업을 위해 노출 여부와 관계없이 활성 게시글을 조회한다. */
    @EntityGraph(attributePaths = "user")
    @Query("""
            SELECT b FROM Board b
            WHERE b.id = :id
              AND b.deletedAt IS NULL
            """)
    Optional<Board> findActiveById(@Param("id") Long id);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    /** 동시 조회에서도 증가 값을 잃지 않도록 조회수를 직접 증가시킨다. */
    @Query("""
            UPDATE Board b
            SET b.viewCount = b.viewCount + 1
            WHERE b.id = :id
              AND b.deletedAt IS NULL
              AND b.visible = true
            """)
    int incrementViewCount(@Param("id") Long id);
}
