package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.config.AiQuestionProperties;
import com.seoul.market.seoulmarketprice.ai.dto.PriceRankingResponse;
import com.seoul.market.seoulmarketprice.location.dto.DongRegionResponse;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NaturalLanguageSearchServiceDongRankingTest {
    @Test
    void preservesRankingConditionWhenResolvingDongOnlyQuestion() {
        LocationMasterService locationService = mock(LocationMasterService.class);
        RankingSearchService rankingService = mock(RankingSearchService.class);
        when(locationService.resolveDong("대치동")).thenReturn(List.of(
                new DongRegionResponse("대치동", "대치동", "1168010600", "강남구", "11680")
        ));
        when(rankingService.searchStructured(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(
                new PriceRankingResponse("강남구 대치동", "thing_amt", "2026-08-24", null, List.of()));
        NaturalLanguageSearchService service = new NaturalLanguageSearchService(
                new QuestionIntentClassifier(new AiQuestionProperties(List.of("아파트"))),
                mock(AiSearchService.class), mock(SingleRegionSearchService.class),
                mock(DistrictSummarySearchService.class), mock(DistrictRankingSearchService.class),
                mock(TopBottomSearchService.class),
                rankingService, mock(TradeTrendSearchService.class), locationService,
                mock(QuestionAnalysisService.class), mock(NearestApartmentPriceSearchService.class));

        var response = service.search("대치동 비싼 아파트 상위 5개 알려줘");

        assertEquals("APARTMENT_RANKING", response.intent());
        verify(rankingService).searchStructured(org.mockito.ArgumentMatchers.eq("강남구 대치동 비싼 아파트 상위 5개 알려줘"),
                org.mockito.ArgumentMatchers.any());
    }
}
