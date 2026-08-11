package com.seoul.market.seoulmarketprice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** MinIO 서버 접속과 첨부파일 버킷 설정을 바인딩한다. */
@ConfigurationProperties(prefix = "minio")
public record MinioProperties(
        /** S3 호환 API를 제공하는 MinIO endpoint. */
        String endpoint,
        /** MinIO 서비스 계정 Access Key. 빈 값이면 익명 접근을 사용한다. */
        String accessKey,
        /** MinIO 서비스 계정 Secret Key. 빈 값이면 익명 접근을 사용한다. */
        String secretKey,
        /** 게시판 첨부파일 객체를 저장하는 버킷 이름. */
        String bucket
) {
}
