package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.config.AiQuestionProperties;
import com.seoul.market.seoulmarketprice.ai.dto.PriceComparisonResponse;
import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NaturalLanguageSearchServiceNearestApartmentTest {
    @Test
    void routesStructuredNearestApartmentIntent() {
        QuestionAnalysisService analyzer = mock(QuestionAnalysisService.class);
        NearestApartmentPriceSearchService nearest = mock(NearestApartmentPriceSearchService.class);
        QuestionAnalysisResponse analysis = new QuestionAnalysisResponse("NEAREST_APARTMENT_PRICE", List.of(),
                new QuestionAnalysisResponse.AnalyzedPlace("홍대입구", "STATION"), "APARTMENT",
                null, null, 1, null, List.of("LATEST_PRICE"), List.of(), List.of());
        PriceComparisonResponse expected = new PriceComparisonResponse("가장 가까운 아파트입니다.", List.of(), List.of());
        when(analyzer.analyze("홍대입구에서 가장 가까운 아파트 가격 알려줘")).thenReturn(analysis);
        when(nearest.search(analysis)).thenReturn(expected);
        NaturalLanguageSearchService service = new NaturalLanguageSearchService(
                new QuestionIntentClassifier(new AiQuestionProperties(List.of("아파트", "가격"))),
                mock(AiSearchService.class), mock(SingleRegionSearchService.class),
                mock(DistrictSummarySearchService.class), mock(DistrictRankingSearchService.class),
                mock(TopBottomSearchService.class), mock(RankingSearchService.class),
                mock(TradeTrendSearchService.class), mock(LocationMasterService.class), analyzer, nearest);

        var response = service.search("홍대입구에서 가장 가까운 아파트 가격 알려줘");

        assertThat(response.status().name()).isEqualTo("SUCCESS");
        assertThat(response.intent()).isEqualTo("NEAREST_APARTMENT_PRICE");
        assertThat(response.result()).isEqualTo(expected);
    }

    @Test
    void fallsBackLocallyWhenDeployedAnalyzerDoesNotHaveEndpoint() {
        QuestionAnalysisService analyzer = mock(QuestionAnalysisService.class);
        NearestApartmentPriceSearchService nearest = mock(NearestApartmentPriceSearchService.class);
        when(analyzer.analyze("홍대입구에서 가장 가까운 아파트 가격 알려줘"))
                .thenThrow(new IllegalStateException("404 Not Found"));
        when(nearest.search(org.mockito.ArgumentMatchers.any())).thenReturn(
                new PriceComparisonResponse("로컬 보조 분석 성공", List.of(), List.of()));
        NaturalLanguageSearchService service = new NaturalLanguageSearchService(
                new QuestionIntentClassifier(new AiQuestionProperties(List.of("아파트", "가격"))),
                mock(AiSearchService.class), mock(SingleRegionSearchService.class),
                mock(DistrictSummarySearchService.class), mock(DistrictRankingSearchService.class),
                mock(TopBottomSearchService.class), mock(RankingSearchService.class),
                mock(TradeTrendSearchService.class), mock(LocationMasterService.class), analyzer, nearest);

        var response = service.search("홍대입구에서 가장 가까운 아파트 가격 알려줘");

        assertThat(response.status().name()).isEqualTo("SUCCESS");
        assertThat(response.intent()).isEqualTo("NEAREST_APARTMENT_PRICE");
        org.mockito.Mockito.verify(nearest).search(org.mockito.ArgumentMatchers.argThat(value ->
                value.referencePlace().name().equals("홍대입구")
                        && value.referencePlace().type().equals("STATION")));
    }
}
