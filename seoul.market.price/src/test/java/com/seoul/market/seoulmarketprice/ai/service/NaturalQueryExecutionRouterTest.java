package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import com.seoul.market.seoulmarketprice.ai.dto.PriceComparisonResponse;
import com.seoul.market.seoulmarketprice.ai.dto.PriceRankingResponse;
import com.seoul.market.seoulmarketprice.ai.dto.SingleRegionPriceResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NaturalQueryExecutionRouterTest {

    @Test
    void routesStructuredApartmentFiltersToGroundedApartmentDataSearch() {
        RankingSearchService rankingSearchService = mock(RankingSearchService.class);
        ApartmentDataRagSearchService apartmentDataRagSearchService = mock(ApartmentDataRagSearchService.class);
        NaturalQueryExecutionRouter router = router(rankingSearchService, apartmentDataRagSearchService,
                mock(ApartmentDetailSearchService.class), mock(TopBottomSearchService.class));
        QuestionAnalysisResponse analysis = analysis("APARTMENT_RANKING",
                new QuestionAnalysisResponse.SearchFilters(20.0, 29.0, null, null));
        PriceRankingResponse expected = new PriceRankingResponse("강남구 대치동", "average_price", "2026-08-31", null, List.of());
        when(apartmentDataRagSearchService.search(eq("대치동 20평대 아파트 알려줘"), eq(analysis))).thenReturn(expected);

        Object actual = router.executeApartmentRanking("대치동 20평대 아파트 알려줘", analysis, true);

        assertSame(expected, actual);
        verify(apartmentDataRagSearchService).search("대치동 20평대 아파트 알려줘", analysis);
        verifyNoInteractions(rankingSearchService);
    }

    @Test
    void routesApartmentDetailToDetailService() {
        ApartmentDetailSearchService detailSearchService = mock(ApartmentDetailSearchService.class);
        NaturalQueryExecutionRouter router = router(mock(RankingSearchService.class),
                mock(ApartmentDataRagSearchService.class), detailSearchService, mock(TopBottomSearchService.class));
        QuestionAnalysisResponse analysis = analysis("APARTMENT_DETAIL",
                new QuestionAnalysisResponse.SearchFilters(null, null, null, null));
        PriceComparisonResponse expected = new PriceComparisonResponse("단지 상세", List.of(), List.of());
        when(detailSearchService.search(analysis)).thenReturn(expected);

        Object actual = router.executeApartmentDetail(analysis);

        assertSame(expected, actual);
        verify(detailSearchService).search(analysis);
    }

    @Test
    void routesLegacyTopBottomIntentToTopBottomService() {
        TopBottomSearchService topBottomSearchService = mock(TopBottomSearchService.class);
        NaturalQueryExecutionRouter router = router(mock(RankingSearchService.class),
                mock(ApartmentDataRagSearchService.class), mock(ApartmentDetailSearchService.class), topBottomSearchService);
        SingleRegionPriceResponse expected = new SingleRegionPriceResponse("하위 단지", List.of(), List.of());
        when(topBottomSearchService.search("강동구에서 가장 싼 아파트 찾아줘")).thenReturn(expected);

        Object actual = router.executeLegacy(QuestionIntentClassifier.Intent.TOP_BOTTOM, "강동구에서 가장 싼 아파트 찾아줘");

        assertSame(expected, actual);
        verify(topBottomSearchService).search("강동구에서 가장 싼 아파트 찾아줘");
    }

    private NaturalQueryExecutionRouter router(RankingSearchService rankingSearchService,
                                                ApartmentDataRagSearchService apartmentDataRagSearchService,
                                                ApartmentDetailSearchService apartmentDetailSearchService,
                                                TopBottomSearchService topBottomSearchService) {
        return new NaturalQueryExecutionRouter(
                mock(AiSearchService.class), mock(SingleRegionSearchService.class), mock(DistrictSummarySearchService.class),
                mock(CitySummarySearchService.class), mock(DistrictRankingSearchService.class), topBottomSearchService,
                rankingSearchService, mock(TradeTrendSearchService.class), mock(NearestApartmentPriceSearchService.class),
                mock(NearbyApartmentRankingSearchService.class), apartmentDetailSearchService,
                mock(FilteredRegionSummarySearchService.class), apartmentDataRagSearchService);
    }

    private QuestionAnalysisResponse analysis(String intent, QuestionAnalysisResponse.SearchFilters filters) {
        return new QuestionAnalysisResponse(intent, List.of(), null, "APARTMENT", null, null, null, 10,
                null, List.of(), List.of(), List.of(), null, List.of(), filters, false);
    }
}
