package com.seoul.market.seoulmarketprice.ai.dto;

import java.util.List;

public record PlaceResolutionResponse(
        String status,
        String message,
        PlaceCandidate resolvedPlace,
        List<PlaceCandidate> candidates
) {
    public record PlaceCandidate(
            String providerId,
            String name,
            String type,
            String address,
            String roadAddress,
            double latitude,
            double longitude,
            String source
    ) {}

    public static PlaceResolutionResponse resolved(PlaceCandidate place) {
        return new PlaceResolutionResponse("RESOLVED", null, place, List.of());
    }

    public static PlaceResolutionResponse clarification(List<PlaceCandidate> candidates) {
        return new PlaceResolutionResponse("NEED_CLARIFICATION",
                "같은 이름의 장소가 여러 개 있습니다. 기준 장소를 선택해주세요.", null, candidates);
    }

    public static PlaceResolutionResponse notFound(String name) {
        return new PlaceResolutionResponse("NOT_FOUND",
                name + "을(를) 서울에서 찾을 수 없습니다.", null, List.of());
    }
}
