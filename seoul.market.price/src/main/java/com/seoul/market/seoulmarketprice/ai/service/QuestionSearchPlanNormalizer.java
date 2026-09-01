package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates one canonical search plan from an LLM analysis and explicit wording in the original question.
 * Explicit user conditions always take precedence over missing or contradictory LLM fields.
 */
@Component
public class QuestionSearchPlanNormalizer {
    private static final Pattern PYEONG_RANGE = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:~|-|`|∼)\\s*(\\d+(?:\\.\\d+)?)\\s*평(?:대)?");
    private static final Pattern PYEONG_BAND = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*평대");
    private static final Pattern EOK_BAND = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*억대");

    public QuestionAnalysisResponse normalize(String question, QuestionAnalysisResponse source) {
        if (source == null) return null;
        String compact = question == null ? "" : question.replaceAll("\\s+", "");
        QuestionAnalysisResponse.SearchFilters filters = filters(compact, source.filters());
        boolean apartmentQuestion = compact.contains("아파트");
        boolean averageRequest = containsAny(compact, "평균가격", "평균가", "평균거래가", "평균");
        String direction = priceDirection(compact, source.direction());
        String intent = source.intent();
        String target = source.target();
        List<String> toolPlan = source.toolPlan();
        List<String> missingFields = source.missingFields();
        boolean requiresClarification = Boolean.TRUE.equals(source.requiresClarification());

        boolean hasFilters = hasFilters(filters);
        boolean hasPriceDirection = containsAny(compact, "싼", "저렴", "낮은", "최저", "가성비", "비싼", "높은", "최고", "고가");
        boolean hasPlace = source.referencePlace() != null && source.referencePlace().name() != null
                && !source.referencePlace().name().isBlank();
        if (!"APARTMENT_DETAIL".equals(intent) && apartmentQuestion && !hasPlace) {
            if (averageRequest && hasFilters) {
                intent = "SINGLE_REGION";
                target = "REGION";
                toolPlan = List.of("RESOLVE_REGION", "GET_FILTERED_REGION_SUMMARY");
                missingFields = List.of();
                requiresClarification = false;
            } else if (hasFilters || hasPriceDirection) {
                intent = "APARTMENT_RANKING";
                target = "APARTMENT";
                toolPlan = List.of("RESOLVE_REGION", "GET_APARTMENT_RANKING");
                missingFields = List.of();
                requiresClarification = false;
            }
        }
        String metric = ("APARTMENT_RANKING".equals(intent) || "SINGLE_REGION".equals(intent)) && apartmentQuestion
                ? "AVERAGE_PRICE" : source.metric();
        return new QuestionAnalysisResponse(intent, source.regions(), source.referencePlace(), target,
                source.apartmentName(), metric, direction, source.limit(), source.period(), source.requestedMetrics(),
                toolPlan, missingFields, source.ambiguousConcept(), source.metricCandidates(), filters,
                requiresClarification);
    }

    /**
     * Builds a plan without calling the LLM when the user already supplied executable filters or price direction.
     * This keeps simple data-search requests available even when the analysis provider is unavailable or uncertain.
     */
    public QuestionAnalysisResponse fromExplicitQuestion(String question) {
        QuestionAnalysisResponse empty = new QuestionAnalysisResponse("UNSUPPORTED", List.of(), null, null,
                null, null, null, 5, null, List.of(), List.of(), List.of(), null, List.of(),
                new QuestionAnalysisResponse.SearchFilters(null, null, null, null), false);
        QuestionAnalysisResponse plan = normalize(question, empty);
        return "UNSUPPORTED".equals(plan.intent()) ? null : plan;
    }

    private QuestionAnalysisResponse.SearchFilters filters(String question,
                                                            QuestionAnalysisResponse.SearchFilters source) {
        Double minPyeong = source == null ? null : source.minPyeong();
        Double maxPyeong = source == null ? null : source.maxPyeong();
        Long minPrice = source == null ? null : source.minPriceWon();
        Long maxPrice = source == null ? null : source.maxPriceWon();
        Matcher range = PYEONG_RANGE.matcher(question);
        if (range.find()) {
            minPyeong = Double.valueOf(range.group(1));
            maxPyeong = Double.valueOf(range.group(2));
        } else {
            Matcher band = PYEONG_BAND.matcher(question);
            if (band.find()) {
                minPyeong = Double.valueOf(band.group(1));
                maxPyeong = minPyeong + 9;
            }
        }
        Matcher priceBand = EOK_BAND.matcher(question);
        if (priceBand.find()) {
            minPrice = Math.round(Double.parseDouble(priceBand.group(1)) * 100_000_000L);
            maxPrice = minPrice + 100_000_000L;
        }
        return new QuestionAnalysisResponse.SearchFilters(minPyeong, maxPyeong, minPrice, maxPrice);
    }

    private String priceDirection(String question, String sourceDirection) {
        if (containsAny(question, "싼", "저렴", "낮은", "최저", "가성비")) return "ASC";
        if (containsAny(question, "비싼", "높은", "최고", "고가")) return "DESC";
        return sourceDirection;
    }

    private boolean hasFilters(QuestionAnalysisResponse.SearchFilters filters) {
        return filters.minPyeong() != null || filters.maxPyeong() != null
                || filters.minPriceWon() != null || filters.maxPriceWon() != null;
    }

    private boolean containsAny(String value, String... words) {
        for (String word : words) {
            if (value.contains(word)) return true;
        }
        return false;
    }
}
