package com.seoul.market.seoulmarketprice.attachment.dto;

/** 비공개 MinIO 객체를 내려받기 위한 단기 서명 URL 응답이다. */
public record AttachmentDownloadResponse(
        /** 클라이언트가 파일을 내려받을 수 있는 presigned URL. */
        String url,
        /** URL이 유효한 남은 시간(초). */
        long expiresInSeconds
) {
}
