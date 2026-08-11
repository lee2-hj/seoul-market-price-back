package com.seoul.market.seoulmarketprice.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 애플리케이션 설정값을 이용해 공통으로 재사용할 MinIO SDK 클라이언트를 구성한다. */
@Configuration
public class MinioConfig {
    /**
     * 설정된 endpoint로 MinIO 클라이언트를 생성하고 Access Key와 Secret Key가 모두 있을 때만
     * 자격증명을 적용한다. 개발 서버가 익명 접근을 허용하는 경우 두 값을 비워 둘 수 있다.
     *
     * @param properties endpoint와 선택적 자격증명이 바인딩된 설정
     * @return 애플리케이션에서 공유하는 MinIO 클라이언트
     */
    @Bean
    MinioClient minioClient(MinioProperties properties) {
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(properties.endpoint());
        if (hasText(properties.accessKey()) && hasText(properties.secretKey())) {
            builder.credentials(properties.accessKey(), properties.secretKey());
        }
        return builder.build();
    }

    /** 자격증명 적용 여부를 결정하기 위해 null·빈 문자열·공백 문자열이 아닌지 확인한다. */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
