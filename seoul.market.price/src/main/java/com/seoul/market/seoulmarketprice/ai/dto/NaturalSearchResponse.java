package com.seoul.market.seoulmarketprice.ai.dto;

import java.util.List;

public record NaturalSearchResponse(
        NaturalSearchStatus status,
        String intent,
        String message,
        Object result,
        List<String> missingFields,
        List<NaturalRegionCandidate> candidates,
        List<NaturalApartmentCandidate> apartmentCandidates,
        NaturalSearchErrorCode errorCode,
        SearchInterpretation interpretation
) {
    public static NaturalSearchResponse success(String intent, Object result) {
        return new NaturalSearchResponse(NaturalSearchStatus.SUCCESS, intent, null, result,
                List.of(), List.of(), List.of(), null, null);
    }
    public static NaturalSearchResponse success(String intent, Object result,
                                                SearchInterpretation interpretation) {
        return new NaturalSearchResponse(NaturalSearchStatus.SUCCESS, intent, null, result,
                List.of(), List.of(), List.of(), null, interpretation);
    }
    public static NaturalSearchResponse clarification(String intent, String message,
                                                       List<String> missingFields,
                                                       List<NaturalRegionCandidate> candidates) {
        return new NaturalSearchResponse(NaturalSearchStatus.NEED_CLARIFICATION, intent, message, null,
                missingFields, candidates, List.of(), null, null);
    }
    public static NaturalSearchResponse apartmentClarification(String message, List<NaturalApartmentCandidate> candidates) {
        return new NaturalSearchResponse(NaturalSearchStatus.NEED_CLARIFICATION, "APARTMENT_DETAIL", message, null,
                List.of("apartment"), List.of(), candidates, null, null);
    }
    public static NaturalSearchResponse error(String message, NaturalSearchErrorCode code) {
        return new NaturalSearchResponse(NaturalSearchStatus.ERROR, null, message, null,
                List.of(), List.of(), List.of(), code, null);
    }
}
