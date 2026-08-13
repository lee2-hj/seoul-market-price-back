package com.seoul.market.seoulmarketprice.location.service;

import com.seoul.market.seoulmarketprice.location.client.KakaoRegionClient;
import com.seoul.market.seoulmarketprice.location.dto.CurrentDistrictResponse;
import com.seoul.market.seoulmarketprice.location.dto.KakaoRegionResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/** 좌표를 서울 자치구 이름으로 변환한다. */
@Service
public class LocationService {
    private final KakaoRegionClient kakaoRegionClient;

    public LocationService(KakaoRegionClient kakaoRegionClient) {
        this.kakaoRegionClient = kakaoRegionClient;
    }

    public CurrentDistrictResponse findCurrentDistrict(
            double latitude,
            double longitude
    ) {
        validateCoordinates(latitude, longitude);
        KakaoRegionResponse response = kakaoRegionClient.getRegion(latitude, longitude);
        List<KakaoRegionResponse.Document> documents = response.documents();
        KakaoRegionResponse.Document region = documents == null
                ? null
                : documents.stream()
                        .filter(document -> "H".equals(document.regionType()))
                        .findFirst()
                        .orElseGet(() -> documents.stream().findFirst().orElse(null));

        if (region == null || region.region2DepthName() == null) {
            throw new IllegalArgumentException("현재 위치의 행정구역을 확인할 수 없습니다.");
        }
        if (!("서울특별시".equals(region.region1DepthName())
                || "서울".equals(region.region1DepthName()))) {
            throw new IllegalArgumentException("현재 위치가 서울 지역이 아닙니다.");
        }
        return new CurrentDistrictResponse(region.region2DepthName());
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("위도 또는 경도 값이 올바르지 않습니다.");
        }
    }
}
