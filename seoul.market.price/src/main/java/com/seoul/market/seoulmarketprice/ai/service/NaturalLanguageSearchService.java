package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.*;
import com.seoul.market.seoulmarketprice.location.dto.DongRegionResponse;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NaturalLanguageSearchService {
    private static final Pattern FULL_REGION = Pattern.compile("[가-힣]+구\\s+[가-힣]+동");
    private static final Pattern DONG = Pattern.compile("([가-힣]+동)");
    private final QuestionIntentClassifier classifier;
    private final AiSearchService comparisonService;
    private final SingleRegionSearchService singleRegionService;
    private final TopBottomSearchService topBottomService;
    private final LocationMasterService locationService;

    public NaturalLanguageSearchService(QuestionIntentClassifier classifier, AiSearchService comparisonService,
                                        SingleRegionSearchService singleRegionService,
                                        TopBottomSearchService topBottomService,
                                        LocationMasterService locationService) {
        this.classifier = classifier;
        this.comparisonService = comparisonService;
        this.singleRegionService = singleRegionService;
        this.topBottomService = topBottomService;
        this.locationService = locationService;
    }

    public NaturalSearchResponse search(String question) {
        try {
            classifier.validateScope(question);
            String normalizedQuestion = resolveDongOnlyQuestion(question);
            if (normalizedQuestion == null) return buildClarification(question);
            QuestionIntentClassifier.Intent intent = classifier.classify(normalizedQuestion);
            Object result = switch (intent) {
                case PRICE_COMPARISON -> comparisonService.search(normalizedQuestion);
                case SINGLE_REGION -> singleRegionService.search(normalizedQuestion);
                case TOP_BOTTOM -> topBottomService.search(normalizedQuestion);
            };
            return NaturalSearchResponse.success(intent.name(), result);
        } catch (IllegalArgumentException exception) {
            return NaturalSearchResponse.error(exception.getMessage(), errorCode(exception.getMessage()));
        } catch (Exception exception) {
            return NaturalSearchResponse.error("AI 검색을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.",
                    NaturalSearchErrorCode.AI_UNAVAILABLE);
        }
    }

    private String resolveDongOnlyQuestion(String question) {
        if (FULL_REGION.matcher(question).find()) return question;
        List<String> dongs = dongNames(question);
        if (dongs.isEmpty()) return question;
        List<DongRegionResponse> selected = new ArrayList<>();
        for (String dong : dongs) {
            List<DongRegionResponse> candidates = locationService.resolveDong(dong);
            if (candidates.size() != 1) return null;
            selected.add(candidates.get(0));
        }
        if (selected.size() == 1) {
            DongRegionResponse region = selected.get(0);
            return region.sggName() + " " + region.dongName() + " 가격 알려줘";
        }
        DongRegionResponse first = selected.get(0), second = selected.get(1);
        return first.sggName() + " " + first.dongName() + "과 " + second.sggName() + " "
                + second.dongName() + " 가격 비교해줘";
    }

    private NaturalSearchResponse buildClarification(String question) {
        List<String> dongs = dongNames(question);
        List<NaturalRegionCandidate> candidates = new ArrayList<>();
        for (int slot = 0; slot < dongs.size(); slot++) {
            for (DongRegionResponse candidate : locationService.resolveDong(dongs.get(slot))) {
                candidates.add(new NaturalRegionCandidate(slot, candidate.requestedName(), candidate.sggName(),
                        candidate.sggCode(), candidate.dongName(), candidate.dongCode()));
            }
        }
        return NaturalSearchResponse.clarification(dongs.size() > 1 ? "PRICE_COMPARISON" : "SINGLE_REGION",
                "같은 이름의 동이 여러 자치구에 있습니다. 지역을 선택해주세요.",
                List.of("sgg"), candidates);
    }

    private List<String> dongNames(String question) {
        Matcher matcher = DONG.matcher(question);
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
