package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Merges a previous turn only when the current utterance explicitly depends on it.
 * A newly extracted slot always wins; a new scope also clears incompatible old scopes.
 */
@Component
public class ConversationContextMerger {
    private static final Logger log = LoggerFactory.getLogger(ConversationContextMerger.class);

    private static final Pattern EXPLICIT_CONTEXT_REFERENCE = Pattern.compile(
            ".*(?:\\uADF8\\uAC70\\uAE30|\\uADF8\\uACF3|\\uADF8\\s*\\uC9C0\\uC5ED|\\uADF8\\s*\\uC8FC\\uBCC0|"
                    + "\\uAC19\\uC740\\s*\\uC870\\uAC74|\\uB3D9\\uC77C\\uD55C?\\s*\\uC870\\uAC74|\\uC774\\uC5B4\\uC11C).*");

    /** Legacy overload. Without the original utterance, context is deliberately not inherited. */
    public MergeResult merge(QuestionAnalysisResponse current, QuestionAnalysisResponse previous) {
        return merge(null, current, previous);
    }

    public MergeResult merge(String question, QuestionAnalysisResponse current, QuestionAnalysisResponse previous) {
        if (previous == null || current == null) return new MergeResult(current, List.of());

        boolean explicitReference = hasExplicitContextReference(question);
        boolean hasNewScope = hasScope(current);
        boolean conditionOnlyFollowUp = !hasNewScope && hasContinuationCondition(current);
        boolean mayInherit = explicitReference || conditionOnlyFollowUp;
        String inheritanceReason = explicitReference ? "explicit-reference"
                : conditionOnlyFollowUp ? "condition-only-follow-up" : "new-or-independent-request";

        List<String> inherited = new ArrayList<>();
        List<String> decisions = new ArrayList<>();

        List<QuestionAnalysisResponse.AnalyzedRegion> regions = chooseRegions(
                current.regions(), previous.regions(), mayInherit, inherited, decisions);
        // A newly stated administrative region starts a new geographic scope.  Do not combine
        // it with an old station/place merely because the user also used a referring phrase.
        boolean mayInheritPlace = mayInherit && isEmpty(current.regions()) && isBlank(current.apartmentName());
        QuestionAnalysisResponse.AnalyzedPlace place = choosePlace(
                current.referencePlace(), previous.referencePlace(), mayInheritPlace, inherited, decisions);
        String apartmentName = chooseText("apartmentName", current.apartmentName(), previous.apartmentName(),
                mayInherit, inherited, decisions);
        QuestionAnalysisResponse.SearchFilters filters = mergeFilters(current.filters(), previous.filters(),
                mayInherit, inherited, decisions);
        String metric = chooseText("metric", current.metric(), previous.metric(), mayInherit, inherited, decisions);
        String direction = chooseText("direction", current.direction(), previous.direction(), mayInherit, inherited, decisions);

        // Intent is always decided per current turn. It is not a conversational slot.
        decisions.add("intent=current");
        String intent = current.intent();
        String target = current.target();
        decisions.add("target=current");

        List<String> missing = recomputeMissing(intent, current, regions, place, apartmentName, metric, filters);
        QuestionAnalysisResponse merged = new QuestionAnalysisResponse(intent, safe(regions), place,
                target, apartmentName, metric, direction, current.limit(), current.period(),
                current.requestedMetrics(), current.toolPlan(), missing, current.ambiguousConcept(),
                current.metricCandidates(), filters, !missing.isEmpty());

        log.info("AI context slot merge: inheritanceAllowed={}, reason={}, decisions={}, inherited={}",
                mayInherit, inheritanceReason, decisions, inherited);
        return new MergeResult(merged, List.copyOf(inherited));
    }

    private List<QuestionAnalysisResponse.AnalyzedRegion> chooseRegions(
            List<QuestionAnalysisResponse.AnalyzedRegion> current,
            List<QuestionAnalysisResponse.AnalyzedRegion> previous,
            boolean mayInherit, List<String> inherited, List<String> decisions) {
        if (!isEmpty(current)) {
            decisions.add("region=current");
            return current;
        }
        if (mayInherit && !isEmpty(previous)) {
            inherited.add("region");
            decisions.add("region=previous");
            return previous;
        }
        decisions.add("region=empty");
        return List.of();
    }

    private QuestionAnalysisResponse.AnalyzedPlace choosePlace(
            QuestionAnalysisResponse.AnalyzedPlace current,
            QuestionAnalysisResponse.AnalyzedPlace previous,
            boolean mayInherit, List<String> inherited, List<String> decisions) {
        if (!isBlank(current == null ? null : current.name())) {
            decisions.add("referencePlace=current");
            return current;
        }
        if (mayInherit && previous != null && !isBlank(previous.name())) {
            inherited.add("referencePlace");
            decisions.add("referencePlace=previous");
            return previous;
        }
        decisions.add("referencePlace=empty");
        return null;
    }

    private QuestionAnalysisResponse.SearchFilters mergeFilters(QuestionAnalysisResponse.SearchFilters current,
                                                                  QuestionAnalysisResponse.SearchFilters previous,
                                                                  boolean mayInherit, List<String> inherited,
                                                                  List<String> decisions) {
        QuestionAnalysisResponse.SearchFilters now = current == null
                ? new QuestionAnalysisResponse.SearchFilters(null, null, null, null) : current;
        QuestionAnalysisResponse.SearchFilters before = previous == null
                ? new QuestionAnalysisResponse.SearchFilters(null, null, null, null) : previous;
        Double minPyeong = chooseFilter("filters.minPyeong", now.minPyeong(), before.minPyeong(), mayInherit, inherited, decisions);
        Double maxPyeong = chooseFilter("filters.maxPyeong", now.maxPyeong(), before.maxPyeong(), mayInherit, inherited, decisions);
        Long minPrice = chooseFilter("filters.minPriceWon", now.minPriceWon(), before.minPriceWon(), mayInherit, inherited, decisions);
        Long maxPrice = chooseFilter("filters.maxPriceWon", now.maxPriceWon(), before.maxPriceWon(), mayInherit, inherited, decisions);
        return new QuestionAnalysisResponse.SearchFilters(minPyeong, maxPyeong, minPrice, maxPrice);
    }

    private <T> T chooseFilter(String slot, T current, T previous, boolean mayInherit,
                               List<String> inherited, List<String> decisions) {
        if (current != null) {
            decisions.add(slot + "=current");
            return current;
        }
        if (mayInherit && previous != null) {
            inherited.add(slot);
            decisions.add(slot + "=previous");
            return previous;
        }
        decisions.add(slot + "=empty");
        return null;
    }

    private String chooseText(String slot, String current, String previous, boolean mayInherit,
                              List<String> inherited, List<String> decisions) {
        if (!isBlank(current)) {
            decisions.add(slot + "=current");
            return current;
        }
        if (mayInherit && !isBlank(previous)) {
            inherited.add(slot);
            decisions.add(slot + "=previous");
            return previous;
        }
        decisions.add(slot + "=empty");
        return null;
    }

    private boolean hasExplicitContextReference(String question) {
        return question != null && EXPLICIT_CONTEXT_REFERENCE.matcher(question.trim()).matches();
    }

    /** A criterion-only utterance such as "most expensive first" is a valid follow-up. */
    private boolean hasContinuationCondition(QuestionAnalysisResponse current) {
        return !hasScope(current)
                && (hasFilters(current.filters()) || !isBlank(current.metric()) || !isBlank(current.direction()));
    }

    private boolean hasScope(QuestionAnalysisResponse current) {
        return !isEmpty(current.regions()) || !isBlank(current.referencePlace() == null ? null : current.referencePlace().name())
                || !isBlank(current.apartmentName());
    }

    private List<String> recomputeMissing(String intent, QuestionAnalysisResponse current,
                                          List<QuestionAnalysisResponse.AnalyzedRegion> regions,
                                          QuestionAnalysisResponse.AnalyzedPlace place,
                                          String apartmentName, String metric,
                                          QuestionAnalysisResponse.SearchFilters filters) {
        List<String> missing = new ArrayList<>(current.missingFields() == null ? List.of() : current.missingFields());
        if (!isEmpty(regions)) missing.remove("region");
        if (place != null && !isBlank(place.name())) missing.remove("referencePlace");
        if (!isBlank(apartmentName)) missing.remove("apartmentName");
        if (!isBlank(metric)) missing.remove("metric");
        if (filters != null && (filters.minPyeong() != null || filters.maxPyeong() != null
                || filters.minPriceWon() != null || filters.maxPriceWon() != null)) missing.remove("filters");
        if (isBlank(intent) && !missing.contains("intent")) missing.add("intent");
        if (!isBlank(intent) && (intent.contains("RANKING") || intent.contains("SUMMARY"))
                && !intent.contains("NEARBY") && isEmpty(regions) && !missing.contains("region")) missing.add("region");
        if (!isBlank(intent) && intent.contains("NEARBY") && place == null && !missing.contains("referencePlace")) missing.add("referencePlace");
        if (!isBlank(intent) && intent.contains("RANKING") && isBlank(metric) && !missing.contains("metric")) missing.add("metric");
        if ("APARTMENT_DETAIL".equals(intent) && isBlank(apartmentName) && !missing.contains("apartmentName")) missing.add("apartmentName");
        return List.copyOf(missing);
    }

    private boolean isEmpty(List<?> values) { return values == null || values.isEmpty(); }
    private boolean hasFilters(QuestionAnalysisResponse.SearchFilters filters) {
        return filters != null && (filters.minPyeong() != null || filters.maxPyeong() != null
                || filters.minPriceWon() != null || filters.maxPriceWon() != null);
    }
    private <T> List<T> safe(List<T> values) { return values == null ? List.of() : values; }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    public record MergeResult(QuestionAnalysisResponse analysis, List<String> inheritedFromContext) {}
}
