package com.seoul.market.seoulmarketprice.ai.dto;

import java.util.List;

public record TradeTrendFacts(String region, String periodStart, String periodEnd, int totalDealCount,
                              long averageTradeAmount, String averageTradeAmountUnit, Double volumeChangeRate,
                              List<TopApartment> topApartmentsByVolume,
                              List<PyeongDistribution> pyeongDistribution) {
    public record TopApartment(String name, int dealCount, Long averageTradeAmount) {}
    public record PyeongDistribution(String pyeongGroup, int dealCount, double ratio) {}
}
