package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.config.AiQuestionProperties;
import com.seoul.market.seoulmarketprice.ai.dto.SingleRegionPriceResponse;
import com.seoul.market.seoulmarketprice.ai.query.DataSourceAdapterRegistry;
import com.seoul.market.seoulmarketprice.ai.query.ScopeResolverChain;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NaturalLanguageSearchServiceCitySummaryTest {
    @Test
    void routesExplicitSeoulWideAverageQuestionWithoutCallingLlmAnalyzer() {
        QuestionAnalysisService analyzer = mock(QuestionAnalysisService.class);
        CitySummarySearchService citySummary = mock(CitySummarySearchService.class);
        SingleRegionPriceResponse expected = new SingleRegionPriceResponse("서울시 전체 평균 거래가입니다.", List.of(), List.of());
        when(citySummary.search("서울시 아파트 전체 평균가격 알려줘")).thenReturn(expected);

        NaturalQueryExecutionRouter router = new NaturalQueryExecutionRouter(
                mock(AiSearchService.class), mock(SingleRegionSearchService.class), mock(DistrictSummarySearchService.class),
                citySummary, mock(DistrictRankingSearchService.class), mock(TopBottomSearchService.class),
                mock(RankingSearchService.class), mock(TradeTrendSearchService.class),
                mock(NearestApartmentPriceSearchService.class), mock(NearbyApartmentRankingSearchService.class),
                mock(ApartmentDetailSearchService.class), mock(FilteredRegionSummarySearchService.class),
                mock(ApartmentDataRagSearchService.class));
        NaturalLanguageSearchService service = new NaturalLanguageSearchService(
                new QuestionIntentClassifier(new AiQuestionProperties(List.of("아파트"))),
                mock(LocationMasterService.class), analyzer, null, null, null,
                new QuestionSearchPlanNormalizer(), new ScopeResolverChain(List.of()),
                new DataSourceAdapterRegistry(List.of()), router, new PreferenceRegionResolver(null, null));

        var response = service.search("서울시 아파트 전체 평균가격 알려줘");

        assertThat(response.intent()).isEqualTo("CITY_SUMMARY");
        assertThat(response.result()).isEqualTo(expected);
        verify(citySummary).search("서울시 아파트 전체 평균가격 알려줘");
        verifyNoInteractions(analyzer);
    }
}
