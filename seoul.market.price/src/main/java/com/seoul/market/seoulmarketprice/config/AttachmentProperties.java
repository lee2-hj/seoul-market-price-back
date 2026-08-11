package com.seoul.market.seoulmarketprice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 게시글 첨부파일의 개수와 용량 제한 설정을 바인딩한다. */
@ConfigurationProperties(prefix = "app.attachment")
public record AttachmentProperties(
        /** 게시글 하나에 등록할 수 있는 최대 파일 개수. */
        int maxFileCount,
        /** 첨부파일 하나가 가질 수 있는 최대 바이트 크기. */
        long maxFileSize,
        /** 게시글 하나의 모든 첨부파일을 합한 최대 바이트 크기. */
        long maxTotalSize
) {
}
