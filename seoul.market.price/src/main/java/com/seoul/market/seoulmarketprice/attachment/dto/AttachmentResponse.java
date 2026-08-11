package com.seoul.market.seoulmarketprice.attachment.dto;

import com.seoul.market.seoulmarketprice.attachment.entity.Attachment;

import java.time.LocalDateTime;

/** 화면에 노출해도 되는 첨부파일 메타데이터 응답이다. */
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
    /** MinIO 내부 객체 키를 제외하고 엔티티를 공개 응답으로 변환한다. */
    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(attachment.getId(), attachment.getOriginalName(),
                attachment.getContentType(), attachment.getFileSize(), attachment.getCreatedAt());
    }
}
