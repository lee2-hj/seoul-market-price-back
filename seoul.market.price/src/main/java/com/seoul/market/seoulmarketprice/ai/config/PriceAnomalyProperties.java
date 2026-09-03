package com.seoul.market.seoulmarketprice.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.price-anomaly")
public record PriceAnomalyProperties(double lowerRatio, double upperRatio, long cacheTtlMinutes) {}
