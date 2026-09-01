package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import com.seoul.market.seoulmarketprice.ai.dto.SingleRegionPriceResponse;
import com.seoul.market.seoulmarketprice.ai.query.GenericQueryExecutor;
import com.seoul.market.seoulmarketprice.ai.query.MetricRecord;
import com.seoul.market.seoulmarketprice.ai.query.QueryRequest;
import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocation;
import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocationRepository;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Calculates a transaction-count weighted regional average after applying explicit search filters. */
@Service
public class FilteredRegionSummarySearchService {
    private static final BigDecimal WON_PER_MANWON = BigDecimal.valueOf(10_000);
    private static final double SQUARE_METERS_PER_PYEONG = 3.305785;

    private final ApartmentLocationRepository apartmentLocationRepository;
    private final RankingRegionResolver regionResolver;
    private final GenericQueryExecutor queryExecutor;

    @Autowired
    public FilteredRegionSummarySearchService(ApartmentLocationRepository apartmentLocationRepository,
                                              SggMasterRepository sggRepository,
                                              LocationMasterService locationService,
                                              GenericQueryExecutor queryExecutor) {
        this.apartmentLocationRepository = apartmentLocationRepository;
        this.regionResolver = new RankingRegionResolver(sggRepository, locationService);
        this.queryExecutor = queryExecutor;
    }

    /** Preserves direct unit-test construction while Spring injects the shared executor. */
    FilteredRegionSummarySearchService(ApartmentLocationRepository apartmentLocationRepository,
                                       SggMasterRepository sggRepository,
                                       LocationMasterService locationService) {
        this(apartmentLocationRepository, sggRepository, locationService, new GenericQueryExecutor());
    }

    public SingleRegionPriceResponse search(String question, QuestionAnalysisResponse analysis) {
        if (!apartmentLocationRepository.isAvailable()) {
            throw new IllegalArgumentException("조건부 지역 평균 조회용 아파트 데이터셋에 연결할 수 없습니다.");
        }
        RankingRegionResolver.ResolvedRegion region = regionResolver.resolve(question);
        if (region.allSeoul()) {
            throw new IllegalArgumentException("면적·가격 조건이 있는 평균 조회는 현재 자치구 또는 자치동을 지정해주세요.");
        }
        QuestionAnalysisResponse.SearchFilters filters = analysis.filters();
        String[] names = region.name().split("\\s+", 2);
        String districtName = names[0];
        String dongName = region.dongCode() == null || names.length < 2 ? null : names[1];
        List<ApartmentLocation> source = apartmentLocationRepository.findByRegion(region.sggCode(), region.dongCode());
        if (source.isEmpty()) {
            source = apartmentLocationRepository.findByRegionName(districtName, dongName);
        }
        List<MetricRecord> matched = queryExecutor.execute(source.stream()
                        .map(this::toMetricRecord)
                        .filter(java.util.Objects::nonNull)
                        .toList(),
                new QueryRequest(1L,
                        filters == null ? null : filters.minPyeong(),
                        filters == null ? null : filters.maxPyeong(),
                        filters == null ? null : filters.minPriceWon(),
                        exclusiveUpperBound(filters == null ? null : filters.maxPriceWon()),
                        QueryRequest.SortField.AVERAGE_PRICE, false, Integer.MAX_VALUE));
        long transactionCount = matched.stream().mapToLong(item -> item.tradeCount() == null ? 0 : item.tradeCount()).sum();
        if (transactionCount == 0) {
            throw new IllegalArgumentException(region.name() + "에서 요청한 가격·면적 조건을 만족하는 아파트 거래 데이터를 찾을 수 없습니다.");
        }

        long averagePrice = weightedAverage(matched, false, transactionCount);
        long averagePyeongPrice = weightedAverage(matched, true, transactionCount);
        String baseDate = matched.stream().map(MetricRecord::baseDate).filter(value -> value != null && !value.isBlank())
                .max(Comparator.naturalOrder()).orElse(null);
        String condition = conditionLabel(filters);
        String displayRegion = region.name() + (condition.isBlank() ? "" : " · " + condition);
        String amount = NumberFormat.getIntegerInstance(Locale.KOREA).format(averagePrice);
        return new SingleRegionPriceResponse(
                displayRegion + "의 조회 데이터 기준 평균 거래가는 " + amount + "만원입니다.",
                List.of("조건: " + (condition.isBlank() ? "전체" : condition),
                        "거래 건수: " + NumberFormat.getIntegerInstance(Locale.KOREA).format(transactionCount) + "건",
                        "평균 평단가: " + NumberFormat.getIntegerInstance(Locale.KOREA).format(averagePyeongPrice) + "만원/평"),
                List.of("평균 거래가 · 만원 · " + (baseDate == null ? "조회 데이터 기준" : baseDate + " 기준"),
                        "단지별 평균 거래가를 거래 건수로 가중 평균한 값입니다."));
    }

    private MetricRecord toMetricRecord(ApartmentLocation item) {
        if (item.averageTradeAmount() == null || item.dealCount() == null || item.dealCount() < 1) return null;
        Double pyeong = item.exclusiveAreaM2() == null ? null : item.exclusiveAreaM2() / SQUARE_METERS_PER_PYEONG;
        return new MetricRecord(item.apartmentId(), item.districtName(), item.dongName(), item.apartmentName(),
                item.averageTradeAmount() * WON_PER_MANWON.longValue(), item.averagePyeongAmount(),
                item.exclusiveAreaM2(), pyeong, item.dealCount().longValue(), item.latestDealDate(), item.baseDate());
    }

    private Long exclusiveUpperBound(Long value) {
        return value == null ? null : value - 1;
    }

    private long weightedAverage(List<MetricRecord> values, boolean pyeong, long transactionCount) {
        long weightedSum = values.stream().mapToLong(item -> {
            Long value = pyeong ? item.averagePyeongPriceManwon()
                    : item.averagePriceWon() == null ? null : item.averagePriceWon() / WON_PER_MANWON.longValue();
            return value == null ? 0 : value * item.tradeCount();
        }).sum();
        return Math.round((double) weightedSum / transactionCount);
    }

    private String conditionLabel(QuestionAnalysisResponse.SearchFilters filters) {
        if (filters == null) return "";
        if (filters.minPyeong() != null && filters.maxPyeong() != null) {
            return number(filters.minPyeong()) + "~" + number(filters.maxPyeong()) + "평";
        }
        if (filters.minPriceWon() != null && filters.maxPriceWon() != null) {
            return BigDecimal.valueOf(filters.minPriceWon()).divide(BigDecimal.valueOf(100_000_000)) + "억대";
        }
        return "";
    }

    private String number(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
