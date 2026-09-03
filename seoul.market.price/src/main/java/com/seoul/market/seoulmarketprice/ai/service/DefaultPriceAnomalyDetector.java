package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.config.PriceAnomalyProperties;
import com.seoul.market.seoulmarketprice.ai.dto.PriceRankingResponse;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class DefaultPriceAnomalyDetector implements PriceAnomalyDetector {
    private static final Logger log = LoggerFactory.getLogger("ai.anomaly");
    private final RegionPriceBaselineProvider baselineProvider;
    private final SggMasterRepository sggRepository;
    private final PriceAnomalyProperties properties;

    public DefaultPriceAnomalyDetector(RegionPriceBaselineProvider baselineProvider, SggMasterRepository sggRepository,
                                       PriceAnomalyProperties properties) {
        this.baselineProvider = baselineProvider;
        this.sggRepository = sggRepository;
        this.properties = properties;
    }
    @Override public List<AnomalyWarning> checkRankingItems(String sggCode, List<PriceRankingResponse.Item> items) {
        return items == null ? List.of() : items.stream().filter(item -> item.metricValue() != null)
                .map(item -> checkSingleValue(sggCode, item.apartmentName(), item.metricValue() * 10_000L)
                        .map(warning -> item.dealCount() <= 1 ? withDealContext(warning) : warning))
                .flatMap(Optional::stream).toList();
    }
    @Override public Optional<AnomalyWarning> checkSingleValue(String sggCode, String subject, long value) {
        return baselineProvider.baseline(sggCode).flatMap(baseline -> {
            if (baseline.averageTradeAmountWon() <= 0) return Optional.empty();
            double ratio = (double) value / baseline.averageTradeAmountWon();
            if (ratio >= properties.lowerRatio() && ratio <= properties.upperRatio()) return Optional.empty();
            String guName = sggRepository.findBySggCode(sggCode).map(item -> item.getSggName()).orElse("해당 자치구");
            String message = subject + "의 평균 거래가(" + money(value) + ")가 " + guName + " 평균(약 "
                    + money(baseline.averageTradeAmountWon()) + ") 대비 " + String.format(Locale.ROOT, "%.2f", ratio)
                    + "배로 차이가 있어 확인이 필요할 수 있습니다.";
            log.warn("price_anomaly sggCode={} subject={} value={} baseline={} ratio={}", sggCode, subject, value,
                    baseline.averageTradeAmountWon(), ratio);
            return Optional.of(new AnomalyWarning(subject, value, baseline.averageTradeAmountWon(), ratio, message));
        });
    }
    private AnomalyWarning withDealContext(AnomalyWarning warning) {
        return new AnomalyWarning(warning.subject(), warning.value(), warning.baselineValue(), warning.ratio(),
                warning.message() + " 거래 1건 기준입니다.");
    }
    private String money(long won) { return NumberFormat.getIntegerInstance(Locale.KOREA).format(won / 10_000L) + "만원"; }
}
