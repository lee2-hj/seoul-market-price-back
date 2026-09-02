package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import org.springframework.stereotype.Component;

/**
 * Executes the query families that already share the common scope/adapter/executor contract.
 * NaturalLanguageSearchService keeps responsibility for analysis, clarification, and response shaping.
 */
@Component
public class NaturalQueryExecutionRouter {
    private final AiSearchService comparisonService;
    private final SingleRegionSearchService singleRegionService;
    private final DistrictSummarySearchService districtSummarySearchService;
    private final CitySummarySearchService citySummarySearchService;
    private final DistrictRankingSearchService districtRankingSearchService;
    private final TopBottomSearchService topBottomSearchService;
    private final RankingSearchService rankingSearchService;
    private final TradeTrendSearchService tradeTrendSearchService;
    private final NearestApartmentPriceSearchService nearestApartmentPriceSearchService;
    private final NearbyApartmentRankingSearchService nearbyApartmentRankingSearchService;
    private final ApartmentDetailSearchService apartmentDetailSearchService;
    private final FilteredRegionSummarySearchService filteredRegionSummarySearchService;
    private final ApartmentDataRagSearchService apartmentDataRagSearchService;

    public NaturalQueryExecutionRouter(AiSearchService comparisonService,
                                       SingleRegionSearchService singleRegionService,
                                       DistrictSummarySearchService districtSummarySearchService,
                                       CitySummarySearchService citySummarySearchService,
                                       DistrictRankingSearchService districtRankingSearchService,
                                       TopBottomSearchService topBottomSearchService,
                                       RankingSearchService rankingSearchService,
                                       TradeTrendSearchService tradeTrendSearchService,
                                       NearestApartmentPriceSearchService nearestApartmentPriceSearchService,
                                       NearbyApartmentRankingSearchService nearbyApartmentRankingSearchService,
                                       ApartmentDetailSearchService apartmentDetailSearchService,
                                       FilteredRegionSummarySearchService filteredRegionSummarySearchService,
                                       ApartmentDataRagSearchService apartmentDataRagSearchService) {
        this.comparisonService = comparisonService;
        this.singleRegionService = singleRegionService;
        this.districtSummarySearchService = districtSummarySearchService;
        this.citySummarySearchService = citySummarySearchService;
        this.districtRankingSearchService = districtRankingSearchService;
        this.topBottomSearchService = topBottomSearchService;
        this.rankingSearchService = rankingSearchService;
        this.tradeTrendSearchService = tradeTrendSearchService;
        this.nearestApartmentPriceSearchService = nearestApartmentPriceSearchService;
        this.nearbyApartmentRankingSearchService = nearbyApartmentRankingSearchService;
        this.apartmentDetailSearchService = apartmentDetailSearchService;
        this.filteredRegionSummarySearchService = filteredRegionSummarySearchService;
        this.apartmentDataRagSearchService = apartmentDataRagSearchService;
    }

    public Object executeApartmentRanking(String question, QuestionAnalysisResponse analysis,
                                          boolean structuredRanking) {
        if (structuredRanking) {
            if (hasStructuredFilters(analysis) && apartmentDataRagSearchService != null) {
                return apartmentDataRagSearchService.search(question, analysis);
            }
            return rankingSearchService.searchStructured(question, analysis);
        }
        return rankingSearchService.search(question);
    }

    public Object executeFilteredSingleRegion(String question, QuestionAnalysisResponse analysis) {
        if (filteredRegionSummarySearchService == null) {
            throw new IllegalStateException("조건부 지역 평균 조회 서비스를 초기화할 수 없습니다.");
        }
        return filteredRegionSummarySearchService.search(question, analysis);
    }

    public Object executeApartmentDetail(QuestionAnalysisResponse analysis) {
        if (apartmentDetailSearchService == null) {
            throw new IllegalStateException("아파트 상세 조회 서비스를 초기화할 수 없습니다.");
        }
        return apartmentDetailSearchService.search(analysis);
    }

    public Object executeNearbyApartmentRanking(QuestionAnalysisResponse analysis) {
        if (nearbyApartmentRankingSearchService == null) {
            throw new IllegalStateException("주변 아파트 순위 조회 서비스를 초기화할 수 없습니다.");
        }
        return nearbyApartmentRankingSearchService.search(analysis);
    }

    public Object executeNearestApartmentPrice(QuestionAnalysisResponse analysis) {
        if (nearestApartmentPriceSearchService == null) {
            throw new IllegalStateException("가장 가까운 아파트 조회 서비스를 초기화할 수 없습니다.");
        }
        return nearestApartmentPriceSearchService.search(analysis);
    }

    public Object executeLegacy(QuestionIntentClassifier.Intent intent, String question) {
        return switch (intent) {
            case PRICE_COMPARISON -> comparisonService.search(question);
            case SINGLE_REGION -> singleRegionService.search(question);
            case DISTRICT_SUMMARY -> districtSummarySearchService.search(question);
            case CITY_SUMMARY -> citySummarySearchService.search(question);
            case DISTRICT_RANKING -> districtRankingSearchService.search(question);
            case TOP_BOTTOM -> topBottomSearchService.search(question);
            case RANKING_SEARCH -> rankingSearchService.search(question);
            case TRADE_TREND -> tradeTrendSearchService.search(question);
        };
    }

    private boolean hasStructuredFilters(QuestionAnalysisResponse analysis) {
        QuestionAnalysisResponse.SearchFilters filters = analysis.filters();
        return filters != null && (filters.minPyeong() != null || filters.maxPyeong() != null
                || filters.minPriceWon() != null || filters.maxPriceWon() != null);
    }
}
