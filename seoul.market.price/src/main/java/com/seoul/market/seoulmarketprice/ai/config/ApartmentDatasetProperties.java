package com.seoul.market.seoulmarketprice.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.datasets.apartment-main")
/** 아파트 위치 데이터셋의 저장 위치와 캐시 정책을 보유한다. */
public record ApartmentDatasetProperties(String mode, String storage, String bucket, String prefix,
                                         Long cacheTtlSeconds) {
    /** 설정값을 조합해 데이터셋의 논리적 저장 위치를 반환한다. */
    public String location() {
        return "s3://" + bucket + "/" + prefix + "/";
    }

    /** 잘못된 캐시 TTL을 기본 1시간으로 보정한다. */
    public long effectiveCacheTtlSeconds() {
        return cacheTtlSeconds == null || cacheTtlSeconds < 1 ? 3600 : cacheTtlSeconds;
    }
}
