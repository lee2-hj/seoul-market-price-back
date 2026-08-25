package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.PlaceResolutionResponse;
import com.seoul.market.seoulmarketprice.location.client.KakaoPlaceClient;
import com.seoul.market.seoulmarketprice.location.dto.KakaoPlaceResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KakaoPlaceResolver implements PlaceResolver {
    private final KakaoPlaceClient client;

    public KakaoPlaceResolver(KakaoPlaceClient client) {
        this.client = client;
    }

    @Override
    public PlaceResolutionResponse resolve(String name, String type) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("장소명을 입력해주세요.");
        String normalizedType = type == null || type.isBlank() ? "UNKNOWN" : type;
        KakaoPlaceResponse response = client.search(name.trim(), normalizedType);
        List<PlaceResolutionResponse.PlaceCandidate> candidates = response.documents() == null ? List.of()
                : response.documents().stream()
                .filter(this::isSeoul)
                .map(document -> toCandidate(document, normalizedType))
                .toList();
        if (candidates.isEmpty()) return PlaceResolutionResponse.notFound(name.trim());
        if (candidates.size() == 1) return PlaceResolutionResponse.resolved(candidates.get(0));
        return PlaceResolutionResponse.clarification(candidates);
    }

    private boolean isSeoul(KakaoPlaceResponse.Document document) {
        return startsWithSeoul(document.addressName()) || startsWithSeoul(document.roadAddressName());
    }

    private boolean startsWithSeoul(String address) {
        return address != null && (address.startsWith("서울특별시") || address.startsWith("서울 "));
    }

    private PlaceResolutionResponse.PlaceCandidate toCandidate(KakaoPlaceResponse.Document document, String type) {
        try {
            return new PlaceResolutionResponse.PlaceCandidate(document.id(), document.placeName(), type,
                    document.addressName(), document.roadAddressName(),
                    Double.parseDouble(document.y()), Double.parseDouble(document.x()), "KAKAO");
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("카카오 장소 좌표 형식이 올바르지 않습니다.", exception);
        }
    }
}
