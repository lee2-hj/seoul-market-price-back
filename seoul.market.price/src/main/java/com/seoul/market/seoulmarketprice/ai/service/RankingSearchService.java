package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.RankingMetric;
import com.seoul.market.seoulmarketprice.ai.dto.RankingSearchQuery;
import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import com.seoul.market.seoulmarketprice.ai.dto.RankingTarget;
import com.seoul.market.seoulmarketprice.ai.dto.SortDirection;
import org.springframework.stereotype.Service;

/** 모든 순위형 질문의 단일 진입점이다. */
@Service
public class RankingSearchService {
    private final RankingQuestionParser questionParser;
    private final TradeVolumeRankingSearchService tradeVolumeService;
    private final PriceRankingSearchService priceService;
    private final AmbiguousMetricResolver ambiguousMetricResolver;

    public RankingSearchService(RankingQuestionParser questionParser,
                                TradeVolumeRankingSearchService tradeVolumeService,
                                PriceRankingSearchService priceService,
                                AmbiguousMetricResolver ambiguousMetricResolver) {
        this.questionParser = questionParser;
        this.tradeVolumeService = tradeVolumeService;
        this.priceService = priceService;
        this.ambiguousMetricResolver = ambiguousMetricResolver;
    }

    public Object search(String question) {
        RankingSearchQuery query = questionParser.parse(question);
        return switch (query.metric()) {
            case TRADE_COUNT -> tradeVolumeService.search(question, query);
            case PRICE -> priceService.search(question, query);
            case CHANGE_RATE, POPULARITY -> throw new IllegalArgumentException(
                    "현재 지원하지 않는 순위 기준입니다. 가격 또는 거래량 기준으로 질문해 주세요.");
        };
    }

    public Object searchStructured(String question, QuestionAnalysisResponse analysis) {
        RankingMetric metric = analysis.ambiguousConcept() == null || analysis.ambiguousConcept().isBlank()
                ? rankingMetric(analysis.metric()) : ambiguousMetricResolver.resolve(analysis).metric();
        QuestionAnalysisResponse.SearchFilters filters = analysis.filters() == null
                ? new QuestionAnalysisResponse.SearchFilters(null, null, null, null) : analysis.filters();
        java.time.LocalDate today = java.time.LocalDate.now();
        RankingSearchQuery query = new RankingSearchQuery(RankingTarget.APARTMENT, metric,
                direction(analysis.direction()), null, today.minusMonths(1), today,
                decimal(filters.minPriceWon()), decimal(filters.maxPriceWon()),
                decimal(filters.minPyeong()), decimal(filters.maxPyeong()), null,
                analysis.limit() == null ? 1 : Math.min(analysis.limit(), 5), 3);
        return switch (metric) {
            case TRADE_COUNT -> tradeVolumeService.search(question, query);
            case PRICE -> priceService.search(question, query);
            case CHANGE_RATE, POPULARITY -> throw new IllegalArgumentException(
                    "현재 데이터로 처리할 수 없는 순위 기준입니다.");
        };
    }

    private RankingMetric rankingMetric(String metric) {
        return "TRADE_COUNT".equals(metric) ? RankingMetric.TRADE_COUNT : RankingMetric.PRICE;
    }

    private SortDirection direction(String direction) {
        return "ASC".equals(direction) ? SortDirection.ASC : SortDirection.DESC;
    }

    private java.math.BigDecimal decimal(Number value) {
        return value == null ? null : new java.math.BigDecimal(value.toString());
    }
}
