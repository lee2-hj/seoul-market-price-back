package com.seoul.market.seoulmarketprice.comment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    /** 프론트엔드가 작성자 실명을 name 필드로 사용할 수 있도록 기존 값을 함께 제공한다. */
    @JsonProperty("name")
    public String name() {
        return writerName;
    }
}
