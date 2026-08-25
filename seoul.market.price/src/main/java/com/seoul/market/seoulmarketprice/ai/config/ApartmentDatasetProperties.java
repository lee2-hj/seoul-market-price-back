package com.seoul.market.seoulmarketprice.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.datasets.apartment-main")
public record ApartmentDatasetProperties(String mode, String storage, String bucket, String prefix,
                                         Long cacheTtlSeconds) {
    public String location() {
        return "s3://" + bucket + "/" + prefix + "/";
    }

    public long effectiveCacheTtlSeconds() {
        return cacheTtlSeconds == null || cacheTtlSeconds < 1 ? 3600 : cacheTtlSeconds;
    }
}
