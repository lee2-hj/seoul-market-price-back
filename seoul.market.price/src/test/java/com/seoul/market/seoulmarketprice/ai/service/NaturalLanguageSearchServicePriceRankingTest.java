package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.config.AiQuestionProperties;
import com.seoul.market.seoulmarketprice.ai.dto.PriceRankingResponse;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NaturalLanguageSearchServicePriceRankingTest {
    @Test
    void routesExpensiveApartmentQuestionToPriceRankingService() {
        RankingSearchService rankingService = mock(RankingSearchService.class);
        PriceRankingResponse expected = new PriceRankingResponse("서울 전체", "thing_amt", "2026-08-24", null, List.of());
        when(rankingService.search(anyString())).thenReturn(expected);
        NaturalLanguageSearchService service = new NaturalLanguageSearchService(
                new QuestionIntentClassifier(new AiQuestionProperties(List.of("아파트"))),
                mock(AiSearchService.class), mock(SingleRegionSearchService.class),
                mock(DistrictSummarySearchService.class), mock(DistrictRankingSearchService.class),
                mock(TopBottomSearchService.class),
                rankingService, mock(TradeTrendSearchService.class), mock(LocationMasterService.class));

        var response = service.search("비싼 아파트 상위 5개 알려줘");

        assertEquals("RANKING_SEARCH", response.intent());
        assertEquals(expected, response.result());
        verify(rankingService).search("비싼 아파트 상위 5개 알려줘");
    }
}
