package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.*;
import com.seoul.market.seoulmarketprice.location.dto.DongRegionResponse;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

@Service
public class NaturalLanguageSearchService {
    private static final Logger log = LoggerFactory.getLogger(NaturalLanguageSearchService.class);
    private final QuestionIntentClassifier classifier;
    private final AiSearchService comparisonService;
    private final SingleRegionSearchService singleRegionService;
    private final DistrictSummarySearchService districtSummaryService;
    private final CitySummarySearchService citySummarySearchService;
    private final DistrictRankingSearchService districtRankingService;
    private final TopBottomSearchService topBottomService;
    private final RankingSearchService rankingSearchService;
    private final TradeTrendSearchService tradeTrendSearchService;
    private final LocationMasterService locationService;
    private final QuestionAnalysisService questionAnalysisService;
    private final NearestApartmentPriceSearchService nearestApartmentPriceSearchService;
    private final NearbyApartmentRankingSearchService nearbyApartmentRankingSearchService;

    @Autowired
    public NaturalLanguageSearchService(QuestionIntentClassifier classifier, AiSearchService comparisonService,
                                        SingleRegionSearchService singleRegionService,
                                        DistrictSummarySearchService districtSummaryService,
                                        CitySummarySearchService citySummarySearchService,
                                        DistrictRankingSearchService districtRankingService,
                                        TopBottomSearchService topBottomService,
                                        RankingSearchService rankingSearchService,
                                        TradeTrendSearchService tradeTrendSearchService,
                                        LocationMasterService locationService,
                                        QuestionAnalysisService questionAnalysisService,
                                        NearestApartmentPriceSearchService nearestApartmentPriceSearchService,
                                        NearbyApartmentRankingSearchService nearbyApartmentRankingSearchService) {
        this.classifier = classifier;
        this.comparisonService = comparisonService;
        this.singleRegionService = singleRegionService;
        this.districtSummaryService = districtSummaryService;
        this.citySummarySearchService = citySummarySearchService;
        this.districtRankingService = districtRankingService;
        this.topBottomService = topBottomService;
        this.rankingSearchService = rankingSearchService;
        this.tradeTrendSearchService = tradeTrendSearchService;
        this.locationService = locationService;
        this.questionAnalysisService = questionAnalysisService;
        this.nearestApartmentPriceSearchService = nearestApartmentPriceSearchService;
        this.nearbyApartmentRankingSearchService = nearbyApartmentRankingSearchService;
    }

    /** 기존 단위 테스트와의 생성자 호환을 위한 보조 생성자. 애플리케이션에서는 주입 생성자를 사용한다. */
    NaturalLanguageSearchService(QuestionIntentClassifier classifier, AiSearchService comparisonService,
                                 SingleRegionSearchService singleRegionService,
                                 DistrictSummarySearchService districtSummaryService,
                                 DistrictRankingSearchService districtRankingService,
                                 TopBottomSearchService topBottomService,
                                 RankingSearchService rankingSearchService,
                                 TradeTrendSearchService tradeTrendSearchService,
                                 LocationMasterService locationService,
                                 QuestionAnalysisService questionAnalysisService,
                                 NearestApartmentPriceSearchService nearestApartmentPriceSearchService) {
        this(classifier, comparisonService, singleRegionService, districtSummaryService, null,
                districtRankingService, topBottomService, rankingSearchService, tradeTrendSearchService, locationService,
                questionAnalysisService, nearestApartmentPriceSearchService, null);
    }

    public NaturalSearchResponse search(String question) {
        try {
            classifier.validateScope(question);
            QuestionAnalysisResponse analysis = analyze(question);
            if (isNearbyApartmentRanking(analysis)) {
                if (nearbyApartmentRankingSearchService == null) {
                    throw new IllegalStateException("주변 아파트 순위 서비스를 초기화할 수 없습니다.");
                }
                return NaturalSearchResponse.success(analysis.intent(),
                        nearbyApartmentRankingSearchService.search(analysis));
            }
            String normalizedQuestion = resolveDongOnlyQuestion(question);
            if (normalizedQuestion == null) {
                String intent = analysis != null && "APARTMENT_RANKING".equals(analysis.intent())
                        ? "APARTMENT_RANKING" : null;
                return buildClarification(question, intent);
            }
            if (analysis != null && "NEAREST_APARTMENT_PRICE".equals(analysis.intent())) {
                return NaturalSearchResponse.success(analysis.intent(),
                        nearestApartmentPriceSearchService.search(analysis));
            }
            if (analysis != null && "APARTMENT_RANKING".equals(analysis.intent())) {
                if (hasAmbiguousConcept(analysis)) {
                    Object result = rankingSearchService.searchStructured(normalizedQuestion, analysis);
                    return NaturalSearchResponse.success(analysis.intent(), result, interpretation(analysis));
                }
                return NaturalSearchResponse.success(analysis.intent(),
                        rankingSearchService.search(normalizedQuestion));
            }
            QuestionIntentClassifier.Intent intent = classifier.classify(normalizedQuestion);
            Object result = switch (intent) {
                case PRICE_COMPARISON -> comparisonService.search(normalizedQuestion);
                case SINGLE_REGION -> singleRegionService.search(normalizedQuestion);
                case DISTRICT_SUMMARY -> districtSummaryService.search(normalizedQuestion);
                case CITY_SUMMARY -> citySummarySearchService.search(normalizedQuestion);
                case DISTRICT_RANKING -> districtRankingService.search(normalizedQuestion);
                case TOP_BOTTOM -> topBottomService.search(normalizedQuestion);
                case RANKING_SEARCH -> rankingSearchService.search(normalizedQuestion);
                case TRADE_TREND -> tradeTrendSearchService.search(normalizedQuestion);
            };
            return NaturalSearchResponse.success(intent.name(), result);
        } catch (IllegalArgumentException exception) {
            return NaturalSearchResponse.error(exception.getMessage(), errorCode(exception.getMessage()));
        } catch (Exception exception) {
            return NaturalSearchResponse.error("AI 검색을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.",
                    NaturalSearchErrorCode.AI_UNAVAILABLE);
        }
    }

    private boolean hasAmbiguousConcept(QuestionAnalysisResponse analysis) {
        return analysis.ambiguousConcept() != null && !analysis.ambiguousConcept().isBlank();
    }

    private boolean isNearbyApartmentRanking(QuestionAnalysisResponse analysis) {
        if (analysis == null || analysis.referencePlace() == null) return false;
        return "NEARBY_APARTMENT_RANKING".equals(analysis.intent())
                || "APARTMENT_RANKING".equals(analysis.intent());
    }

    private SearchInterpretation interpretation(QuestionAnalysisResponse analysis) {
        if (analysis.ambiguousConcept() == null || analysis.ambiguousConcept().isBlank()) return null;
        QuestionAnalysisResponse.MetricCandidate applied = analysis.metricCandidates() == null ? null
                : analysis.metricCandidates().stream()
                .filter(candidate -> "TRADE_COUNT".equals(candidate.metric()))
                .max(java.util.Comparator.comparingDouble(QuestionAnalysisResponse.MetricCandidate::confidence))
                .orElse(null);
        if (applied == null) return null;
        return new SearchInterpretation(conceptLabel(analysis.ambiguousConcept()), "거래 건수",
                applied.reason(), applied.confidence(), true);
    }

    private String conceptLabel(String concept) {
        return switch (concept) {
            case "POPULARITY" -> "인기";
            case "PREFERENCE" -> "선호";
            case "DISLIKE" -> "비선호";
            default -> concept;
        };
    }

    private QuestionAnalysisResponse analyze(String question) {
        try {
            return questionAnalysisService.analyze(question);
        } catch (RuntimeException exception) {
            log.warn("구조화 질문 분석 실패, 기존 검색 분류기로 대체합니다: {}", exception.getMessage());
            return null;
        }
    }


    private String resolveDongOnlyQuestion(String question) {
        if (RegionQuestionPatterns.FULL_REGION.matcher(question).find()) return question;
        List<String> dongs = dongNames(question);
        if (dongs.isEmpty()) return question;
        List<DongRegionResponse> selected = new ArrayList<>();
        for (String dong : dongs) {
            List<DongRegionResponse> candidates = locationService.resolveDong(dong);
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException(dong + "을(를) 찾을 수 없습니다.");
            }
            if (candidates.size() > 1) return null;
            selected.add(candidates.get(0));
        }
        if (selected.size() == 1) {
            DongRegionResponse region = selected.get(0);
            // 동 이름만으로 들어온 질문도 순위·기간 등 원래 검색 조건을 보존한다.
            return region.sggName() + " " + question;
        }
        DongRegionResponse first = selected.get(0), second = selected.get(1);
        return first.sggName() + " " + first.dongName() + "과 " + second.sggName() + " "
                + second.dongName() + " 가격 비교해줘";
    }

    private NaturalSearchResponse buildClarification(String question) {
        return buildClarification(question, null);
    }

    private NaturalSearchResponse buildClarification(String question, String requestedIntent) {
        List<String> dongs = dongNames(question);
        List<NaturalRegionCandidate> candidates = new ArrayList<>();
        for (int slot = 0; slot < dongs.size(); slot++) {
            for (DongRegionResponse candidate : locationService.resolveDong(dongs.get(slot))) {
                candidates.add(new NaturalRegionCandidate(slot, candidate.requestedName(), candidate.sggName(),
                        candidate.sggCode(), candidate.dongName(), candidate.dongCode()));
            }
        }
        String intent = requestedIntent != null ? requestedIntent
                : dongs.size() > 1 ? "PRICE_COMPARISON" : "SINGLE_REGION";
        return NaturalSearchResponse.clarification(intent,
                "같은 이름의 동이 여러 자치구에 있습니다. 지역을 선택해주세요.",
                List.of("sgg"), candidates);
    }

    private List<String> dongNames(String question) {
        Matcher matcher = RegionQuestionPatterns.DONG.matcher(question);
        List<String> names = new ArrayList<>();
        while (matcher.find() && names.size() < 2) names.add(matcher.group(1));
        return names;
    }

    private NaturalSearchErrorCode errorCode(String message) {
        if (message.contains("외 질문")) return NaturalSearchErrorCode.UNSUPPORTED;
        if (message.contains("찾지 못") || message.contains("입력해주세요")) return NaturalSearchErrorCode.MISSING_REGION;
        if (message.contains("데이터가 없")) return NaturalSearchErrorCode.NO_PRICE_DATA;
        return NaturalSearchErrorCode.INVALID_QUESTION;
    }
}
