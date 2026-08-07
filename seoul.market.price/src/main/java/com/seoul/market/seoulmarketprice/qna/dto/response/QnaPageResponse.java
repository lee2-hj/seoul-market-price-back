package com.seoul.market.seoulmarketprice.qna.dto.response;

import java.util.List;

/**
 * Q&A 목록과 페이지 메타데이터를 함께 전달한다.
 *
 * @param content 현재 페이지의 Q&A 목록
 * @param page 현재 페이지 번호
 * @param size 페이지 크기
 * @param totalElements 전체 게시글 수
 * @param totalPages 전체 페이지 수
 * @param first 첫 페이지 여부
 * @param last 마지막 페이지 여부
 */
public record QnaPageResponse(
        List<QnaListResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
