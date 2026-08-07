package com.seoul.market.seoulmarketprice.board.dto.condition;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** URL 쿼리 파라미터로 전달되는 게시판 목록 검색 조건이다. */
@Getter
@Setter
public class BoardSearchCondition {
    /** 조회할 페이지 번호이며 0부터 시작한다. */
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;

    /** 한 페이지에 표시할 게시글 수이다. */
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 10;

    /** 제목·내용 또는 작성자 중 프론트 드롭다운에서 선택한 검색 대상이다. */
    private BoardSearchType searchType;

    /** 선택한 검색 대상에 적용할 검색어이다. */
    @Size(max = 200, message = "검색어는 200자 이하여야 합니다.")
    private String keyword;
}
