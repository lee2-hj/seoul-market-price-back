package com.seoul.market.seoulmarketprice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code app.attachment.*} 설정을 게시글 단위 첨부파일 제한 정책으로 바인딩한다.
 * 용량 값은 모두 바이트 단위이며 일반 게시판과 Q&A 게시판에 동일하게 적용된다.
 */
@ConfigurationProperties(prefix = "app.attachment")
public record AttachmentProperties(
        /** 기존 활성 파일과 새 업로드 파일을 합산한 게시글당 최대 개수. */
        int maxFileCount,
        /** 개별 첨부파일 하나가 가질 수 있는 최대 바이트 크기. */
        long maxFileSize,
        /** 기존 활성 파일과 새 업로드 파일을 합산한 게시글당 최대 바이트 크기. */
        long maxTotalSize
) {
}
