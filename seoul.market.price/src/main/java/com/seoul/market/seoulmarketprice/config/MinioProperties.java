package com.seoul.market.seoulmarketprice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code minio.*} 설정을 MinIO 접속 정보와 첨부파일 저장 버킷으로 바인딩한다.
 * 비밀값은 소스에 직접 작성하지 않고 운영 환경의 환경변수나 외부 설정으로 주입하는 것을 전제로 한다.
 */
@ConfigurationProperties(prefix = "minio")
public record MinioProperties(
        /** S3 호환 API endpoint. 콘솔 주소가 아니라 SDK가 접근하는 API 주소를 사용한다. */
        String endpoint,
        /** MinIO 서비스 계정 Access Key. Secret Key와 함께 비어 있으면 익명 접근을 시도한다. */
        String accessKey,
        /** MinIO 서비스 계정 Secret Key. Access Key와 함께 비어 있으면 익명 접근을 시도한다. */
        String secretKey,
        /** 게시판 첨부파일 객체를 저장하는 사전에 생성된 버킷 이름. */
        String bucket
) {
}
