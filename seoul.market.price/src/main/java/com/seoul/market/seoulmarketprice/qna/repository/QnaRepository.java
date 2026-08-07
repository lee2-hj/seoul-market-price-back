package com.seoul.market.seoulmarketprice.qna.repository;

import com.seoul.market.seoulmarketprice.qna.entity.AnswerStatus;
import com.seoul.market.seoulmarketprice.qna.entity.QnaBoard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/** Q&A 게시글 저장과 화면별 조회 쿼리를 제공한다. */
public interface QnaRepository extends JpaRepository<QnaBoard, Long> {
    /** 공개된 활성 Q&A를 제목 키워드와 답변 상태로 검색한다. */
    @EntityGraph(attributePaths = "user")
    @Query("""
            SELECT q FROM QnaBoard q
            WHERE q.deletedAt IS NULL AND q.publicQuestion = true
              AND (:keyword IS NULL OR LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR q.answerStatus = :status)
            """)
    Page<QnaBoard> findPublicPage(@Param("keyword") String keyword,
                                  @Param("status") AnswerStatus status, Pageable pageable);

    /** 로그인 사용자가 작성한 활성 Q&A를 검색한다. */
    @EntityGraph(attributePaths = "user")
    @Query("""
            SELECT q FROM QnaBoard q
            WHERE q.deletedAt IS NULL AND q.userId = :userId
              AND (:keyword IS NULL OR LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR q.answerStatus = :status)
            """)
    Page<QnaBoard> findMyPage(@Param("userId") Long userId, @Param("keyword") String keyword,
                              @Param("status") AnswerStatus status, Pageable pageable);

    /** 공개 질문 또는 요청 사용자가 작성한 비공개 질문의 상세를 조회한다. */
    @EntityGraph(attributePaths = {"user", "answerMember"})
    @Query("""
            SELECT q FROM QnaBoard q
            WHERE q.id = :id AND q.deletedAt IS NULL
              AND (q.publicQuestion = true OR q.userId = :userId)
            """)
    Optional<QnaBoard> findAccessibleById(@Param("id") Long id, @Param("userId") Long userId);

    /** 관리자 작업 또는 작성자 권한 확인을 위해 활성 질문을 조회한다. */
    @EntityGraph(attributePaths = {"user", "answerMember"})
    @Query("SELECT q FROM QnaBoard q WHERE q.id = :id AND q.deletedAt IS NULL")
    Optional<QnaBoard> findActiveById(@Param("id") Long id);

    /** 백오피스의 복합 검색 조건으로 활성 Q&A 목록을 조회한다. */
    @EntityGraph(attributePaths = {"user", "answerMember"})
    @Query("""
            SELECT q FROM QnaBoard q
            WHERE q.deletedAt IS NULL
              AND (:keyword IS NULL OR LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(q.questionContent) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR q.answerStatus = :status)
              AND (:publicQuestion IS NULL OR q.publicQuestion = :publicQuestion)
              AND (:writer IS NULL OR LOWER(q.user.userId) LIKE LOWER(CONCAT('%', :writer, '%')))
              AND (:from IS NULL OR q.createdAt >= :from)
              AND (:to IS NULL OR q.createdAt < :to)
            """)
    Page<QnaBoard> findAdminPage(@Param("keyword") String keyword, @Param("status") AnswerStatus status,
                                 @Param("publicQuestion") Boolean publicQuestion, @Param("writer") String writer,
                                 @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                 Pageable pageable);

    /** 접근 가능한 상세 게시글의 조회수를 원자적으로 증가시킨다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE QnaBoard q SET q.viewCount = q.viewCount + 1
            WHERE q.id = :id AND q.deletedAt IS NULL
              AND (q.publicQuestion = true OR q.userId = :userId)
            """)
    int incrementViewCount(@Param("id") Long id, @Param("userId") Long userId);
}
