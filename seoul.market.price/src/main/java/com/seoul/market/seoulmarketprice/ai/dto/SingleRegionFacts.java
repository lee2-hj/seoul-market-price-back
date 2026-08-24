package com.seoul.market.seoulmarketprice.ai.dto;

public record SingleRegionFacts(String region, long averagePrice, long averagePyeongPrice,
                                int transactionCount, String baseDate,
                                String averagePriceUnit, String averagePyeongPriceUnit) {}
