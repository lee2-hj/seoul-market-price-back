package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.config.AiQuestionProperties;
import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NaturalLanguageSearchServiceAmbiguousRankingTest {
    @Test
    void routesLlmApartmentRankingWithoutKeywordClassifier() {
        QuestionAnalysisService analyzer = mock(QuestionAnalysisService.class);
        RankingSearchService ranking = mock(RankingSearchService.class);
        String question = "현재 강동구에서 가장 인기있는 아파트는 어디야?";
        var analysis = new QuestionAnalysisResponse("APARTMENT_RANKING", List.of(
                new QuestionAnalysisResponse.AnalyzedRegion("강동구", "DISTRICT")), null,
                "APARTMENT", "UNKNOWN", "DESC", 1, null, List.of("TRADE_COUNT"),
                List.of("GET_APARTMENT_RANKING"), List.of(), "POPULARITY", List.of(
                new QuestionAnalysisResponse.MetricCandidate("TRADE_COUNT", 0.82, "거래 활동 대체 지표")), false);
        Object expected = new Object();
        when(analyzer.analyze(question)).thenReturn(analysis);
        when(ranking.searchStructured(question, analysis)).thenReturn(expected);
        NaturalLanguageSearchService service = new NaturalLanguageSearchService(
                new QuestionIntentClassifier(new AiQuestionProperties(List.of("아파트"))),
                mock(AiSearchService.class), mock(SingleRegionSearchService.class),
                mock(DistrictSummarySearchService.class), mock(DistrictRankingSearchService.class),
                mock(TopBottomSearchService.class), ranking, mock(TradeTrendSearchService.class),
                mock(LocationMasterService.class), analyzer, mock(NearestApartmentPriceSearchService.class));

        var response = service.search(question);

        assertThat(response.intent()).isEqualTo("APARTMENT_RANKING");
        assertThat(response.result()).isSameAs(expected);
        assertThat(response.interpretation()).isNotNull();
        assertThat(response.interpretation().originalConcept()).isEqualTo("POPULARITY");
        assertThat(response.interpretation().appliedMetric()).isEqualTo("거래 건수");
        assertThat(response.interpretation().confidence()).isEqualTo(0.82);
        assertThat(response.interpretation().proxy()).isTrue();
        verify(ranking).searchStructured(question, analysis);
    }
}
