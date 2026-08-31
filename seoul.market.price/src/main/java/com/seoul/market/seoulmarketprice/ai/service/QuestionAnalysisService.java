package com.seoul.market.seoulmarketprice.ai.service;
import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisRequest;
import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class QuestionAnalysisService {
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;
    private final RestClient aiClient;
    public QuestionAnalysisService(@Qualifier("aiFastApiRestClient") RestClient aiClient) { this.aiClient = aiClient; }
    public QuestionAnalysisResponse analyze(String question) {
        if (question == null || question.isBlank()) throw new IllegalArgumentException("질문을 입력해주세요.");
        QuestionAnalysisResponse response = aiClient.post().uri("/ai/analyze-question")
                .body(new QuestionAnalysisRequest(question.trim())).retrieve().body(QuestionAnalysisResponse.class);
        if (response == null) throw new IllegalStateException("질문 분석 서버가 빈 응답을 반환했습니다.");
        return normalize(response);
    }
    private QuestionAnalysisResponse normalize(QuestionAnalysisResponse r) {
        List<String> missing = new ArrayList<>(cleanStrings(r.missingFields()));
        int limit = r.limit() == null ? DEFAULT_LIMIT : r.limit();
        boolean clarify = Boolean.TRUE.equals(r.requiresClarification());
        if (limit < 1 || limit > MAX_LIMIT) { if (!missing.contains("limit")) missing.add("limit"); clarify = true; limit = Math.max(1, Math.min(MAX_LIMIT, limit)); }
        List<QuestionAnalysisResponse.MetricCandidate> candidates = r.metricCandidates() == null ? List.of() : r.metricCandidates().stream()
                .filter(c -> c != null && c.metric() != null && !c.metric().isBlank())
                .map(c -> new QuestionAnalysisResponse.MetricCandidate(c.metric().trim().toUpperCase(Locale.ROOT), Math.max(0, Math.min(1, c.confidence())), c.reason() == null ? "" : c.reason().trim())).toList();
        List<QuestionAnalysisResponse.AnalyzedRegion> regions = normalizeRegions(r.regions());
        QuestionAnalysisResponse.AnalyzedPlace place = normalizePlace(r.referencePlace());
        QuestionAnalysisResponse.SearchFilters filters = normalizeFilters(r.filters(), missing);
        validateRequiredFields(r.intent(), regions, place, r.metric(), missing);
        clarify = clarify || !missing.isEmpty();
        return new QuestionAnalysisResponse(canonical(r.intent()), regions, place, canonical(r.target()), canonical(r.metric()), canonical(r.direction()), limit, clean(r.period()), canonicalStrings(r.requestedMetrics()), canonicalStrings(r.toolPlan()), missing, canonical(r.ambiguousConcept()), candidates, filters, clarify);
    }
    private String clean(String v) { return v == null || v.isBlank() ? null : v.trim(); }
    private List<String> cleanStrings(List<String> values) { return values == null ? List.of() : values.stream().filter(v -> v != null && !v.isBlank()).map(String::trim).toList(); }
    private String canonical(String v) { String value = clean(v); return value == null ? null : value.toUpperCase(Locale.ROOT); }
    private List<String> canonicalStrings(List<String> values) { return cleanStrings(values).stream().map(v -> v.toUpperCase(Locale.ROOT)).toList(); }
    private List<QuestionAnalysisResponse.AnalyzedRegion> normalizeRegions(List<QuestionAnalysisResponse.AnalyzedRegion> values) {
        if (values == null) return List.of();
        return values.stream().filter(v -> v != null && clean(v.name()) != null)
                .map(v -> new QuestionAnalysisResponse.AnalyzedRegion(clean(v.name()), canonical(v.type())))
                .toList();
    }
    private QuestionAnalysisResponse.AnalyzedPlace normalizePlace(QuestionAnalysisResponse.AnalyzedPlace value) {
        if (value == null || clean(value.name()) == null) return null;
        return new QuestionAnalysisResponse.AnalyzedPlace(clean(value.name()), canonical(value.type()));
    }
    private QuestionAnalysisResponse.SearchFilters normalizeFilters(QuestionAnalysisResponse.SearchFilters value,
                                                                     List<String> missing) {
        if (value == null) return new QuestionAnalysisResponse.SearchFilters(null, null, null, null);
        Double minPyeong = value.minPyeong(), maxPyeong = value.maxPyeong();
        Long minPrice = value.minPriceWon(), maxPrice = value.maxPriceWon();
        if ((minPyeong != null && minPyeong < 0) || (maxPyeong != null && maxPyeong < 0)
                || (minPrice != null && minPrice < 0) || (maxPrice != null && maxPrice < 0)
                || (minPyeong != null && maxPyeong != null && minPyeong > maxPyeong)
                || (minPrice != null && maxPrice != null && minPrice > maxPrice)) {
            missing.add("filters");
            return new QuestionAnalysisResponse.SearchFilters(null, null, null, null);
        }
        return value;
    }
    private void validateRequiredFields(String intent, List<QuestionAnalysisResponse.AnalyzedRegion> regions,
                                        QuestionAnalysisResponse.AnalyzedPlace place, String metric, List<String> missing) {
        String normalized = canonical(intent);
        if (normalized == null) { if (!missing.contains("intent")) missing.add("intent"); return; }
        if ((normalized.contains("RANKING") || normalized.contains("SUMMARY")) && regions.isEmpty()
                && !normalized.contains("NEARBY")) {
            if (!missing.contains("region")) missing.add("region");
        }
        if (normalized.contains("NEARBY") && place == null && !missing.contains("referencePlace")) {
            missing.add("referencePlace");
        }
        if (normalized.contains("RANKING") && clean(metric) == null && !missing.contains("metric")) {
            missing.add("metric");
        }
    }
}
