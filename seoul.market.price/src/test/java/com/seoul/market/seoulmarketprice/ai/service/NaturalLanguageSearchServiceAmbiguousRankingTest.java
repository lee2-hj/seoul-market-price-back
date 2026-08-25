package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.config.AiQuestionProperties;
import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import com.seoul.market.seoulmarketprice.location.dto.DongRegionResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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
        assertThat(response.interpretation().originalConcept()).isEqualTo("인기");
        assertThat(response.interpretation().appliedMetric()).isEqualTo("거래 건수");
        assertThat(response.interpretation().confidence()).isEqualTo(0.82);
        assertThat(response.interpretation().proxy()).isTrue();
        verify(ranking).searchStructured(question, analysis);
    }

    @Test
    void resolvesDongOnlyBeforeStructuredPopularityRanking() {
        QuestionAnalysisService analyzer = mock(QuestionAnalysisService.class);
        RankingSearchService ranking = mock(RankingSearchService.class);
        LocationMasterService location = mock(LocationMasterService.class);
        String question = "수유동 인기있는 아파트 찾아줘";
        var analysis = popularityAnalysis("수유동", "DONG");
        when(analyzer.analyze(question)).thenReturn(analysis);
        when(location.resolveDong("수유동")).thenReturn(List.of(
                new DongRegionResponse("수유동", "수유동", "1130510300", "강북구", "11305")));
        Object expected = new Object();
        when(ranking.searchStructured("강북구 " + question, analysis)).thenReturn(expected);
        NaturalLanguageSearchService service = service(analyzer, ranking, location);

        var response = service.search(question);

        assertThat(response.status().name()).isEqualTo("SUCCESS");
        assertThat(response.result()).isSameAs(expected);
        verify(ranking).searchStructured("강북구 " + question, analysis);
        verify(ranking, never()).searchStructured(question, analysis);
    }

    @Test
    void asksForSelectionWhenDongNameHasMultipleRegions() {
        QuestionAnalysisService analyzer = mock(QuestionAnalysisService.class);
        RankingSearchService ranking = mock(RankingSearchService.class);
        LocationMasterService location = mock(LocationMasterService.class);
        String question = "중복동 인기있는 아파트 찾아줘";
        var analysis = popularityAnalysis("중복동", "DONG");
        when(analyzer.analyze(question)).thenReturn(analysis);
        when(location.resolveDong("중복동")).thenReturn(List.of(
                new DongRegionResponse("중복동", "중복동", "1111010100", "종로구", "11110"),
                new DongRegionResponse("중복동", "중복동", "1120010100", "성동구", "11200")));
        NaturalLanguageSearchService service = service(analyzer, ranking, location);

        var response = service.search(question);

        assertThat(response.status().name()).isEqualTo("NEED_CLARIFICATION");
        assertThat(response.intent()).isEqualTo("APARTMENT_RANKING");
        assertThat(response.candidates()).hasSize(2);
        verify(ranking, never()).searchStructured(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotFallbackToSeoulWhenDongDoesNotExist() {
        QuestionAnalysisService analyzer = mock(QuestionAnalysisService.class);
        RankingSearchService ranking = mock(RankingSearchService.class);
        LocationMasterService location = mock(LocationMasterService.class);
        String question = "없는동 인기있는 아파트 찾아줘";
        when(analyzer.analyze(question)).thenReturn(popularityAnalysis("없는동", "DONG"));
        when(location.resolveDong("없는동")).thenReturn(List.of());
        NaturalLanguageSearchService service = service(analyzer, ranking, location);

        var response = service.search(question);

        assertThat(response.status().name()).isEqualTo("ERROR");
        assertThat(response.message()).contains("없는동", "찾을 수 없습니다");
        verify(ranking, never()).searchStructured(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void allowsSeoulRankingOnlyWhenNoRegionWasProvided() {
        QuestionAnalysisService analyzer = mock(QuestionAnalysisService.class);
        RankingSearchService ranking = mock(RankingSearchService.class);
        String question = "서울 전체 인기있는 아파트 찾아줘";
        var analysis = popularityAnalysis("서울 전체", "UNKNOWN");
        when(analyzer.analyze(question)).thenReturn(analysis);
        Object expected = new Object();
        when(ranking.searchStructured(question, analysis)).thenReturn(expected);
        NaturalLanguageSearchService service = service(analyzer, ranking, mock(LocationMasterService.class));

        var response = service.search(question);

        assertThat(response.status().name()).isEqualTo("SUCCESS");
        assertThat(response.result()).isSameAs(expected);
        verify(ranking).searchStructured(question, analysis);
    }

    @Test
    void routesNonAmbiguousPriceConditionToExistingPriceRanking() {
        QuestionAnalysisService analyzer = mock(QuestionAnalysisService.class);
        RankingSearchService ranking = mock(RankingSearchService.class);
        String question = "강남구 100억 이상 아파트";
        var analysis = new QuestionAnalysisResponse("APARTMENT_RANKING", List.of(
                new QuestionAnalysisResponse.AnalyzedRegion("강남구", "DISTRICT")), null,
                "APARTMENT", "AVERAGE_PRICE", null, null, null, List.of("LATEST_PRICE"),
                List.of("GET_APARTMENT_RANKING"), List.of(), null, List.of(), false);
        when(analyzer.analyze(question)).thenReturn(analysis);
        Object expected = new Object();
        when(ranking.search(question)).thenReturn(expected);
        NaturalLanguageSearchService service = service(analyzer, ranking, mock(LocationMasterService.class));

        var response = service.search(question);

        assertThat(response.status().name()).isEqualTo("SUCCESS");
        assertThat(response.result()).isSameAs(expected);
        verify(ranking).search(question);
        verify(ranking, never()).searchStructured(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    private QuestionAnalysisResponse popularityAnalysis(String regionName, String regionType) {
        return new QuestionAnalysisResponse("APARTMENT_RANKING", List.of(
                new QuestionAnalysisResponse.AnalyzedRegion(regionName, regionType)), null,
                "APARTMENT", "UNKNOWN", "DESC", 1, null, List.of("TRADE_COUNT"),
                List.of("GET_APARTMENT_RANKING"), List.of(), "POPULARITY", List.of(
                new QuestionAnalysisResponse.MetricCandidate("TRADE_COUNT", 0.82, "거래량 대체 지표")), false);
    }

    private NaturalLanguageSearchService service(QuestionAnalysisService analyzer,
                                                  RankingSearchService ranking,
                                                  LocationMasterService location) {
        return new NaturalLanguageSearchService(
                new QuestionIntentClassifier(new AiQuestionProperties(List.of("아파트"))),
                mock(AiSearchService.class), mock(SingleRegionSearchService.class),
                mock(DistrictSummarySearchService.class), mock(DistrictRankingSearchService.class),
                mock(TopBottomSearchService.class), ranking, mock(TradeTrendSearchService.class),
                location, analyzer, mock(NearestApartmentPriceSearchService.class));
    }
}
