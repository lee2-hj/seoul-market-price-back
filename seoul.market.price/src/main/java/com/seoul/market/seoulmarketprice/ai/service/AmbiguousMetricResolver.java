package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import com.seoul.market.seoulmarketprice.ai.dto.RankingMetric;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class AmbiguousMetricResolver {
    private static final double AUTO_RESOLVE_CONFIDENCE = 0.70;
    private static final Map<String, MetricDefinition> CATALOG = Map.of(
            "TRADE_COUNT", new MetricDefinition(true, RankingMetric.TRADE_COUNT, "거래 건수"),
            "FAVORITE_COUNT", new MetricDefinition(false, null, "관심 등록 수"),
            "VIEW_COUNT", new MetricDefinition(false, null, "조회수"),
            "NEGATIVE_RATING_COUNT", new MetricDefinition(false, null, "부정 평가 수")
    );

    public Resolution resolve(QuestionAnalysisResponse analysis) {
        List<QuestionAnalysisResponse.MetricCandidate> candidates = analysis.metricCandidates() == null
                ? List.of() : analysis.metricCandidates();
        QuestionAnalysisResponse.MetricCandidate selected = candidates.stream()
                .filter(candidate -> {
                    MetricDefinition definition = CATALOG.get(candidate.metric());
                    return definition != null && definition.available();
                })
                .max(Comparator.comparingDouble(QuestionAnalysisResponse.MetricCandidate::confidence))
                .orElse(null);
        if (selected == null && "TRADE_COUNT".equals(analysis.metric())) {
            return new Resolution(RankingMetric.TRADE_COUNT, "거래 건수", null);
        }
        if (selected == null) {
            throw new IllegalArgumentException(unavailableMessage(analysis.ambiguousConcept(), candidates));
        }
        if (selected.confidence() < AUTO_RESOLVE_CONFIDENCE) {
            throw new IllegalArgumentException("'" + concept(analysis.ambiguousConcept())
                    + "'의 의미를 거래 건수로 단정하기 어렵습니다. 거래량 기준으로 조회할지 명확히 입력해주세요.");
        }
        MetricDefinition definition = CATALOG.get(selected.metric());
        return new Resolution(definition.rankingMetric(), definition.label(), selected.reason());
    }

    private String unavailableMessage(String ambiguousConcept,
                                      List<QuestionAnalysisResponse.MetricCandidate> candidates) {
        String availableCandidates = candidates.stream()
                .map(QuestionAnalysisResponse.MetricCandidate::metric)
                .map(CATALOG::get)
                .filter(definition -> definition != null && !definition.available())
                .map(MetricDefinition::label)
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("직접 측정 지표");
        return "'" + concept(ambiguousConcept) + "'을 판단할 수 있는 " + availableCandidates
                + " 데이터가 없습니다. 이를 거래량이 적은 아파트와 동일한 의미로 해석할 수 없습니다.";
    }

    private String concept(String value) {
        return value == null || value.isBlank() ? "해당 표현" : value;
    }

    public record Resolution(RankingMetric metric, String label, String reason) {}
    private record MetricDefinition(boolean available, RankingMetric rankingMetric, String label) {}
}
