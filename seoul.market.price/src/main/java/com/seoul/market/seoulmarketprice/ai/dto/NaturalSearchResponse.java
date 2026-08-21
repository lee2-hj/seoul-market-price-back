package com.seoul.market.seoulmarketprice.ai.dto;

import java.util.List;

public record NaturalSearchResponse(
        NaturalSearchStatus status,
        String intent,
        String message,
        Object result,
        List<String> missingFields,
        List<NaturalRegionCandidate> candidates,
        NaturalSearchErrorCode errorCode
) {
    public static NaturalSearchResponse success(String intent, Object result) {
        return new NaturalSearchResponse(NaturalSearchStatus.SUCCESS, intent, null, result,
                List.of(), List.of(), null);
    }
    public static NaturalSearchResponse clarification(String intent, String message,
                                                       List<String> missingFields,
                                                       List<NaturalRegionCandidate> candidates) {
        return new NaturalSearchResponse(NaturalSearchStatus.NEED_CLARIFICATION, intent, message, null,
                missingFields, candidates, null);
    }
    public static NaturalSearchResponse error(String message, NaturalSearchErrorCode code) {
        return new NaturalSearchResponse(NaturalSearchStatus.ERROR, null, message, null,
                List.of(), List.of(), code);
    }
}
