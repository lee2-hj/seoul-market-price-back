package com.seoul.market.seoulmarketprice.board.comment.repository;

import com.seoul.market.seoulmarketprice.board.comment.entity.BoardComment;
import com.seoul.market.seoulmarketprice.board.comment.entity.BoardType;
import com.seoul.market.seoulmarketprice.board.comment.entity.WriterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface CommentRepositoryCustom {
    List<BoardComment> findAllByPost(BoardType boardType, Long postId);
    Page<BoardComment> findMyComments(WriterType writerType, Long writerId, Pageable pageable);
    Optional<BoardComment> findByIdAndPost(Long id, BoardType boardType, Long postId);
}
