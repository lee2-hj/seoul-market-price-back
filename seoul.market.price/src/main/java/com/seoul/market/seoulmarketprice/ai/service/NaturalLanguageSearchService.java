package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.*;
import com.seoul.market.seoulmarketprice.location.dto.DongRegionResponse;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import com.seoul.market.seoulmarketprice.ai.query.ApartmentScopeResolver;
import com.seoul.market.seoulmarketprice.ai.query.DataSourceAdapterRegistry;
import com.seoul.market.seoulmarketprice.ai.query.PlaceScopeResolver;
import com.seoul.market.seoulmarketprice.ai.query.RegionScopeResolver;
import com.seoul.market.seoulmarketprice.ai.query.ScopeResolverChain;
import com.seoul.market.seoulmarketprice.ai.query.SearchScope;
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
    private final LocationMasterService locationService;
    private final QuestionAnalysisService questionAnalysisService;
    private final RagAnswerService ragAnswerService;
    private final AiExecutionPlanMapper executionPlanMapper;
    private final AiExecutionPlanValidator executionPlanValidator;
    private final QuestionSearchPlanNormalizer searchPlanNormalizer;
    private final ScopeResolverChain scopeResolverChain;
    private final DataSourceAdapterRegistry dataSourceAdapterRegistry;
    private final NaturalQueryExecutionRouter executionRouter;
    private final PreferenceRegionResolver preferenceRegionResolver;

    @Autowired
    public NaturalLanguageSearchService(QuestionIntentClassifier classifier, LocationMasterService locationService,
                                        QuestionAnalysisService questionAnalysisService,
                                        RagAnswerService ragAnswerService,
                                        AiExecutionPlanMapper executionPlanMapper,
                                        AiExecutionPlanValidator executionPlanValidator,
                                        QuestionSearchPlanNormalizer searchPlanNormalizer,
                                        ScopeResolverChain scopeResolverChain,
                                        DataSourceAdapterRegistry dataSourceAdapterRegistry,
                                        NaturalQueryExecutionRouter executionRouter,
                                        PreferenceRegionResolver preferenceRegionResolver) {
        this.classifier = classifier;
        this.locationService = locationService;
        this.questionAnalysisService = questionAnalysisService;
        this.ragAnswerService = ragAnswerService;
        this.executionPlanMapper = executionPlanMapper;
        this.executionPlanValidator = executionPlanValidator;
        this.searchPlanNormalizer = searchPlanNormalizer;
        this.scopeResolverChain = scopeResolverChain;
        this.dataSourceAdapterRegistry = dataSourceAdapterRegistry;
        this.executionRouter = executionRouter;
        this.preferenceRegionResolver = preferenceRegionResolver;
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
        this(classifier, locationService, questionAnalysisService, null, null, null,
                new QuestionSearchPlanNormalizer(),
                new ScopeResolverChain(List.of(new ApartmentScopeResolver(), new PlaceScopeResolver(), new RegionScopeResolver())),
                new DataSourceAdapterRegistry(List.of()),
                new NaturalQueryExecutionRouter(comparisonService, singleRegionService, districtSummaryService, null,
                        districtRankingService, topBottomService, rankingSearchService, tradeTrendSearchService,
                        nearestApartmentPriceSearchService, null, null, null, null),
                new PreferenceRegionResolver(null, null));
    }

    public NaturalSearchResponse search(String question) {
        return search(question, null);
    }

    public NaturalSearchResponse search(String question, Long memberId) {
        PreferenceRegionResolver.Resolution preferenceResolution = preferenceRegionResolver.resolve(question, memberId);
        if (preferenceResolution.status() == PreferenceRegionResolver.Status.PREFERENCE_UNAVAILABLE) {
            return NaturalSearchResponse.error(PreferenceRegionResolver.PREFERENCE_REGION_REQUIRED_MESSAGE,
                    NaturalSearchErrorCode.MISSING_REGION);
        }
        question = preferenceResolution.question();
        try {
            QuestionAnalysisResponse analyzed = analyze(question);
            // The LLM plan remains the primary source of intent. Explicit wording only corrects
            // its filters/direction; it becomes a fallback when the analyser is unavailable.
            QuestionAnalysisResponse analysis = analyzed == null
                    ? searchPlanNormalizer.fromExplicitQuestion(question)
                    : searchPlanNormalizer.normalize(question, analyzed);
            SearchScope scope = scopeResolverChain.resolve(analysis);
            if (analysis != null && "APARTMENT_DETAIL".equals(analysis.intent())) {
                /*
                if (apartmentDetailSearchService == null) {
                    throw new IllegalStateException("단지 상세 조회 서비스를 초기화할 수 없습니다.");
                }
                */
                return NaturalSearchResponse.success(analysis.intent(), executionRouter.executeApartmentDetail(analysis));
            }
            if (isNearbyApartmentRanking(analysis)) {
                /*
                if (nearbyApartmentRankingSearchService == null) {
                    throw new IllegalStateException("주변 아파트 순위 서비스를 초기화할 수 없습니다.");
                }
                */
                return NaturalSearchResponse.success(analysis.intent(),
                        executionRouter.executeNearbyApartmentRanking(analysis));
            }
            String normalizedQuestion = resolveDongOnlyQuestion(question);
            if (normalizedQuestion == null) {
                String intent = analysis != null && "APARTMENT_RANKING".equals(analysis.intent())
                        ? "APARTMENT_RANKING" : null;
                return buildClarification(question, intent);
            }
            if (analysis != null && "NEAREST_APARTMENT_PRICE".equals(analysis.intent())) {
                return NaturalSearchResponse.success(analysis.intent(),
                        executionRouter.executeNearestApartmentPrice(analysis));
            }
            if (analysis != null && "APARTMENT_RANKING".equals(analysis.intent())) {
                boolean structuredRanking = hasAmbiguousConcept(analysis) || hasStructuredFilters(analysis)
                        || hasExplicitPriceDirection(normalizedQuestion);
                Object result = executionRouter.executeApartmentRanking(normalizedQuestion, analysis, structuredRanking);
                return NaturalSearchResponse.success(analysis.intent(), result,
                        structuredRanking ? interpretation(analysis) : null);
            }
            if (analysis != null && "SINGLE_REGION".equals(analysis.intent()) && hasStructuredFilters(analysis)) {
                return NaturalSearchResponse.success(analysis.intent(),
                        executionRouter.executeFilteredSingleRegion(normalizedQuestion, analysis));
            }
            QuestionIntentClassifier.Intent intent;
            try {
                intent = classifier.classify(normalizedQuestion);
            } catch (IllegalArgumentException exception) {
                String ragAnswer = ragAnswerService == null ? null : ragAnswerService.answerIfSupported(question);
                if (ragAnswer != null) {
                    return NaturalSearchResponse.success("RAG_FAQ", new RagAnswerResponse(ragAnswer));
                }
                throw exception;
            }
            Object result = executionRouter.executeLegacy(intent, normalizedQuestion);
            return NaturalSearchResponse.success(intent.name(), result);
        } catch (ApartmentSelectionRequiredException exception) {
            return NaturalSearchResponse.apartmentClarification(exception.getMessage(), exception.candidates());
        } catch (IllegalArgumentException exception) {
            return NaturalSearchResponse.error(exception.getMessage(), errorCode(exception.getMessage()));
        } catch (Exception exception) {
            log.error("Natural AI search failed. question={}", question, exception);
            return NaturalSearchResponse.error("아파트 조회 데이터를 연결하지 못했습니다. 데이터 서버 상태를 확인한 뒤 다시 시도해주세요.",
                    NaturalSearchErrorCode.AI_UNAVAILABLE);
        }
    }

    private boolean hasAmbiguousConcept(QuestionAnalysisResponse analysis) {
        return analysis.ambiguousConcept() != null && !analysis.ambiguousConcept().isBlank();
    }

    private boolean hasStructuredFilters(QuestionAnalysisResponse analysis) {
        QuestionAnalysisResponse.SearchFilters filters = analysis.filters();
        return filters != null && (filters.minPyeong() != null || filters.maxPyeong() != null
                || filters.minPriceWon() != null || filters.maxPriceWon() != null);
    }

    private boolean hasExplicitPriceDirection(String question) {
        return question != null && (question.contains("싼") || question.contains("저렴") || question.contains("낮은")
                || question.contains("최저") || question.contains("가성비") || question.contains("비싼")
                || question.contains("높은") || question.contains("최고") || question.contains("고가"));
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

    private void validateExecutionPlan(String question, QuestionAnalysisResponse analysis) {
        if (analysis == null || executionPlanMapper == null || executionPlanValidator == null) return;
        AiExecutionPlan plan = executionPlanMapper.map(analysis);
        executionPlanValidator.validate(plan);
        log.info("AI execution plan: question={}, intent={}, scope={}, filters={}, sort={}, tools={}",
                question, plan.intent(), plan.scope(), plan.filters(), plan.sort(), plan.toolPlan());
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
