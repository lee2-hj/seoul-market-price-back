package com.seoul.market.seoulmarketprice.comment.dto.response;
import com.seoul.market.seoulmarketprice.comment.entity.WriterType;

import java.time.LocalDateTime;
import java.util.List;

/** 최상위 댓글과 대댓글 계층을 프론트엔드에 전달하는 응답이다. */
public record CommentResponse(
        Long id,
        Long parentId,
        WriterType writerType,
        Long writerId,
        String writerName,
        String content,
        boolean visible,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CommentResponse> replies
) {
}
