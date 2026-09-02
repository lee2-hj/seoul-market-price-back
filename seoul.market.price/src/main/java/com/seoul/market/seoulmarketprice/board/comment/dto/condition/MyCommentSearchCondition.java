package com.seoul.market.seoulmarketprice.board.comment.dto.condition;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/** 마이페이지에서 본인이 작성한 댓글을 조회할 때 사용하는 페이징 조건이다. */
@Getter
@Setter
public class MyCommentSearchCondition {

    /** 조회할 페이지 번호이며 0부터 시작한다. */
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;

    /** 한 페이지에 표시할 댓글 수이며 최대 100개까지 허용한다. */
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 20;
}
