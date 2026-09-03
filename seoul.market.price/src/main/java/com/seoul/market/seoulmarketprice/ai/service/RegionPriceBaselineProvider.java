package com.seoul.market.seoulmarketprice.ai.service;

import java.util.Optional;

public interface RegionPriceBaselineProvider {
    Optional<Baseline> baseline(String sggCode);
    record Baseline(long averageTradeAmountWon, int sampleDealCount) {}
}
