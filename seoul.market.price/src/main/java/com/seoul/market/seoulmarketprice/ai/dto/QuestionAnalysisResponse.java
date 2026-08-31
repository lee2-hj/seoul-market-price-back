package com.seoul.market.seoulmarketprice.ai.dto;

import java.util.List;

/** LLM이 생성한 조회 계획이며 아직 DB나 외부 도구를 실행한 결과는 아니다. */
public record QuestionAnalysisResponse(
        String intent,
        List<AnalyzedRegion> regions,
        AnalyzedPlace referencePlace,
        String target,
        String apartmentName,
        String metric,
        String direction,
        Integer limit,
        String period,
        List<String> requestedMetrics,
        List<String> toolPlan,
        List<String> missingFields,
        String ambiguousConcept,
        List<MetricCandidate> metricCandidates,
        SearchFilters filters,
        Boolean requiresClarification
) {
    public QuestionAnalysisResponse(String intent, List<AnalyzedRegion> regions, AnalyzedPlace referencePlace,
                                    String target, String metric, String direction, Integer limit, String period,
                                    List<String> requestedMetrics, List<String> toolPlan,
                                    List<String> missingFields, String ambiguousConcept,
                                    List<MetricCandidate> metricCandidates, Boolean requiresClarification) {
        this(intent, regions, referencePlace, target, null, metric, direction, limit, period, requestedMetrics, toolPlan,
                missingFields, ambiguousConcept, metricCandidates, new SearchFilters(null, null, null, null),
                requiresClarification);
    }

    public QuestionAnalysisResponse(String intent, List<AnalyzedRegion> regions, AnalyzedPlace referencePlace,
                                    String target, String metric, String direction, Integer limit, String period,
                                    List<String> requestedMetrics, List<String> toolPlan,
                                    List<String> missingFields) {
        this(intent, regions, referencePlace, target, null, metric, direction, limit, period,
                requestedMetrics, toolPlan, missingFields, null, List.of(), new SearchFilters(null, null, null, null), false);
    }

    public record AnalyzedRegion(String name, String type) {}
    public record AnalyzedPlace(String name, String type) {}
    public record MetricCandidate(String metric, double confidence, String reason) {}
    public record SearchFilters(Double minPyeong, Double maxPyeong,
                                Long minPriceWon, Long maxPriceWon) {}
}
