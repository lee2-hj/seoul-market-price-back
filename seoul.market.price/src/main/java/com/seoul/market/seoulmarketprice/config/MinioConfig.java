package com.seoul.market.seoulmarketprice.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 애플리케이션에서 공통으로 사용할 MinIO SDK 클라이언트를 구성한다. */
@Configuration
public class MinioConfig {
    /** 설정된 endpoint와 선택적 자격증명으로 MinIO 클라이언트를 생성한다. */
    @Bean
    MinioClient minioClient(MinioProperties properties) {
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(properties.endpoint());
        if (hasText(properties.accessKey()) && hasText(properties.secretKey())) {
            builder.credentials(properties.accessKey(), properties.secretKey());
        }
        return builder.build();
    }

    /** null, 빈 문자열, 공백 문자열을 제외한 설정값인지 확인한다. */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
