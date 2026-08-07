package com.seoul.market.seoulmarketprice.board.dto.condition;

/** 게시판 목록의 검색 대상을 구분한다. */
public enum BoardSearchType {
    /** 게시글 제목과 본문을 함께 검색한다. */
    TITLE_CONTENT,

    /** 일반 사용자 또는 공지 작성 관리자를 검색한다. */
    WRITER
}
