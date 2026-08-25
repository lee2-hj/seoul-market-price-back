package com.seoul.market.seoulmarketprice.ai.dto;

import java.util.List;

/** LLM이 생성한 조회 계획이며 아직 DB나 외부 도구를 실행한 결과는 아니다. */
public record QuestionAnalysisResponse(
        String intent,
        List<AnalyzedRegion> regions,
        AnalyzedPlace referencePlace,
        String target,
        String metric,
        String direction,
        Integer limit,
        String period,
        List<String> requestedMetrics,
        List<String> toolPlan,
        List<String> missingFields
) {
    public record AnalyzedRegion(String name, String type) {}
    public record AnalyzedPlace(String name, String type) {}
}
