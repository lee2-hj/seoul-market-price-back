package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.PriceRankingResponse;
import java.util.List;
import java.util.Optional;

public interface PriceAnomalyDetector {
    List<AnomalyWarning> checkRankingItems(String sggCode, List<PriceRankingResponse.Item> items);
    Optional<AnomalyWarning> checkSingleValue(String sggCode, String subject, long value);
    record AnomalyWarning(String subject, long value, long baselineValue, double ratio, String message) {}
}
