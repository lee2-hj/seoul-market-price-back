package com.seoul.market.seoulmarketprice.qna.dto.response;

import com.seoul.market.seoulmarketprice.qna.entity.AnswerStatus;

import java.time.LocalDateTime;

/**
 * Q&A 목록 한 건에 필요한 요약 정보를 전달한다.
 *
 * @param id 게시글 식별자
 * @param title 질문 제목
 * @param writerLoginId 작성자 로그인 아이디
 * @param writerName 작성자 이름
 * @param answerStatus 답변 상태
 * @param viewCount 조회수
 * @param publicQuestion 공개 여부
 * @param attachmentAvailable 첨부파일 존재 여부
 * @param createdAt 질문 등록 시각
 * @param answeredAt 답변 등록 시각
 */
public record QnaListResponse(
        Long id,
        String title,
        String writerLoginId,
        String writerName,
        AnswerStatus answerStatus,
        int viewCount,
        boolean publicQuestion,
        boolean attachmentAvailable,
        LocalDateTime createdAt,
        LocalDateTime answeredAt
) {
}
