package com.seoul.market.seoulmarketprice.qna.dto.condition;

import com.seoul.market.seoulmarketprice.qna.entity.AnswerStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** 백오피스 Q&A 목록에서 사용하는 복합 검색 조건이다. */
@Getter
@Setter
public class AdminQnaSearchCondition {
    /** 조회할 페이지 번호이며 0부터 시작한다. */
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;

    /** 한 페이지에 표시할 질문 수이다. */
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 20;

    /** 질문 제목 또는 본문에 적용할 검색어이다. */
    @Size(max = 200, message = "검색어는 200자 이하여야 합니다.")
    private String keyword;

    /** 답변대기 또는 답변완료 필터이다. */
    private AnswerStatus status;

    /** 공개 또는 비공개 질문 필터이다. */
    private Boolean publicQuestion;

    /** 작성자 로그인 아이디에 적용할 검색어이다. */
    @Size(max = 50, message = "작성자 검색어는 50자 이하여야 합니다.")
    private String writer;

    /** 질문 작성일 검색 시작일이다. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    /** 질문 작성일 검색 종료일이다. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;
}
