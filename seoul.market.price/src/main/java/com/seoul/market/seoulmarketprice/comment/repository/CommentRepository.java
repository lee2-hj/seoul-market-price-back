package com.seoul.market.seoulmarketprice.comment.repository;

import com.seoul.market.seoulmarketprice.comment.entity.BoardComment;
import com.seoul.market.seoulmarketprice.comment.entity.BoardType;
import com.seoul.market.seoulmarketprice.comment.entity.WriterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** 댓글 저장과 게시글별 댓글 조회를 담당한다. */
public interface CommentRepository extends JpaRepository<BoardComment, Long>, CommentRepositoryCustom {

    /** 게시글의 댓글과 부모 댓글을 오래된 순서로 한 번에 조회한다. */

    /** 로그인 사용자가 작성했고 삭제되지 않은 댓글을 최신순으로 조회한다. */

    /** URL의 게시글 ID와 실제 댓글 소속이 일치하는 댓글만 조회한다. */

    /** 삭제되지 않은 대댓글이 존재하는지 확인한다. */
    boolean existsByParentIdAndDeletedAtIsNull(Long parentId);
}
