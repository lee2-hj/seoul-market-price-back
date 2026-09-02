package com.seoul.market.seoulmarketprice.board.comment.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seoul.market.seoulmarketprice.board.comment.entity.BoardComment;
import com.seoul.market.seoulmarketprice.board.comment.entity.BoardType;
import com.seoul.market.seoulmarketprice.board.comment.entity.QBoardComment;
import com.seoul.market.seoulmarketprice.board.comment.entity.WriterType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class CommentRepositoryCustomImpl implements CommentRepositoryCustom {
    private static final QBoardComment comment = QBoardComment.boardComment;
    private final JPAQueryFactory queryFactory;

    public List<BoardComment> findAllByPost(BoardType boardType, Long postId) {
        return queryFactory.selectFrom(comment).leftJoin(comment.parent).fetchJoin()
                .where(comment.boardType.eq(boardType), comment.postId.eq(postId))
                .orderBy(comment.createdAt.asc(), comment.id.asc()).fetch();
    }

    public Page<BoardComment> findMyComments(WriterType writerType, Long writerId, Pageable pageable) {
        var condition = comment.writerType.eq(writerType)
                .and(comment.writerId.eq(writerId)).and(comment.deletedAt.isNull());
        List<BoardComment> content = queryFactory.selectFrom(comment)
                .leftJoin(comment.parent).fetchJoin().where(condition)
                .orderBy(comment.createdAt.desc(), comment.id.desc())
                .offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();
        Long total = queryFactory.select(comment.count()).from(comment).where(condition).fetchOne();
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    public Optional<BoardComment> findByIdAndPost(Long id, BoardType boardType, Long postId) {
        return Optional.ofNullable(queryFactory.selectFrom(comment)
                .leftJoin(comment.parent).fetchJoin()
                .where(comment.id.eq(id), comment.boardType.eq(boardType), comment.postId.eq(postId))
                .fetchOne());
    }
}
