package com.seoul.market.seoulmarketprice.ai.dto;

public record SearchInterpretation(
        String originalConcept,
        String appliedMetric,
        String reason,
        double confidence,
        boolean proxy
) {}
