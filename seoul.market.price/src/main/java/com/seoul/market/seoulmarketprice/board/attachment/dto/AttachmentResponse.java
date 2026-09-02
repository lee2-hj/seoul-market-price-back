package com.seoul.market.seoulmarketprice.board.attachment.dto;

import com.seoul.market.seoulmarketprice.board.attachment.entity.Attachment;

import java.time.LocalDateTime;

/**
 * 화면에 노출해도 되는 첨부파일 메타데이터 응답이다.
 * 내부 버킷명과 객체 키는 노출하지 않으며 실제 다운로드 주소는 별도 API에서 발급한다.
 */
public record AttachmentResponse(
        /** 첨부파일 메타데이터 식별자. */
        Long id,
        /** 사용자가 업로드한 원본 파일명. */
        String originalName,
        /** 업로드 시 확인한 MIME 타입. */
        String contentType,
        /** 파일 크기(바이트). */
        long fileSize,
        /** 첨부파일 등록 시각. */
        LocalDateTime createdAt
) {
    /**
     * MinIO 내부 객체 키와 삭제 정보를 제외하고 엔티티를 공개 응답으로 변환한다.
     *
     * @param attachment 변환할 활성 첨부파일 엔티티
     * @return 화면에 전달할 첨부파일 메타데이터
     */
    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(attachment.getId(), attachment.getOriginalName(),
                attachment.getContentType(), attachment.getFileSize(), attachment.getCreatedAt());
    }
}
