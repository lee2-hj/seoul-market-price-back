package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.config.AiQuestionProperties;
import com.seoul.market.seoulmarketprice.ai.dto.NaturalSearchResponse;
import com.seoul.market.seoulmarketprice.ai.dto.TradeVolumeRankingResponse;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NaturalLanguageSearchServiceTradeVolumeTest {
    @Test
    void routesTradeVolumeQuestionToRankingService() {
        RankingSearchService rankingService = mock(RankingSearchService.class);
        TradeVolumeRankingResponse expected = new TradeVolumeRankingResponse("강남구", "2026-08-01", "2026-08-24", 10, null, List.of());
        when(rankingService.search(anyString())).thenReturn(expected);

        NaturalLanguageSearchService service = new NaturalLanguageSearchService(
                new QuestionIntentClassifier(new AiQuestionProperties(List.of("아파트", "거래량"))),
                mock(AiSearchService.class), mock(SingleRegionSearchService.class),
                mock(DistrictSummarySearchService.class), mock(DistrictRankingSearchService.class),
                mock(TopBottomSearchService.class),
                rankingService, mock(TradeTrendSearchService.class), mock(LocationMasterService.class));

        NaturalSearchResponse response = service.search("강남구에서 거래량이 많은 아파트 상위 2개 알려줘");

        assertEquals("RANKING_SEARCH", response.intent());
        assertEquals(expected, response.result());
        verify(rankingService).search("강남구에서 거래량이 많은 아파트 상위 2개 알려줘");
    }
}
