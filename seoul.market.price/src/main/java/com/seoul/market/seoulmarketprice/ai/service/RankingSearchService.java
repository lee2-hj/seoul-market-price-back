package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.RankingMetric;
import com.seoul.market.seoulmarketprice.ai.dto.RankingSearchQuery;
import org.springframework.stereotype.Service;

/** 모든 순위형 질문의 단일 진입점이다. */
@Service
public class RankingSearchService {
    private final RankingQuestionParser questionParser;
    private final TradeVolumeRankingSearchService tradeVolumeService;
    private final PriceRankingSearchService priceService;

    public RankingSearchService(RankingQuestionParser questionParser,
                                TradeVolumeRankingSearchService tradeVolumeService,
                                PriceRankingSearchService priceService) {
        this.questionParser = questionParser;
        this.tradeVolumeService = tradeVolumeService;
        this.priceService = priceService;
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
}
