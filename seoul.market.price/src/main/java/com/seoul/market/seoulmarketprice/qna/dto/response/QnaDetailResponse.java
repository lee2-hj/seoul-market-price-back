package com.seoul.market.seoulmarketprice.qna.dto.response;

import com.seoul.market.seoulmarketprice.qna.entity.AnswerStatus;

import java.time.LocalDateTime;

/**
 * Q&A 상세 화면에 질문과 답변 정보를 전달한다.
 *
 * @param id 게시글 식별자
 * @param writerLoginId 작성자 로그인 아이디
 * @param writerName 작성자 이름
 * @param title 질문 제목
 * @param questionContent 질문 본문
 * @param answerContent 관리자 답변 본문
 * @param answerAdminName 답변 관리자 이름
 * @param answerStatus 답변 상태
 * @param viewCount 조회수
 * @param publicQuestion 공개 여부
 * @param createdAt 질문 등록 시각
 * @param updatedAt 최종 수정 시각
 * @param answeredAt 답변 등록 시각
 */
public record QnaDetailResponse(
        Long id,
        String writerLoginId,
        String writerName,
        String title,
        String questionContent,
        String answerContent,
        String answerAdminName,
        AnswerStatus answerStatus,
        int viewCount,
        boolean publicQuestion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime answeredAt
) {
}
