package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.NearbyApartmentRequest;
import com.seoul.market.seoulmarketprice.ai.dto.NearbyApartmentResponse;
import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocation;
import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocationRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class NearbyApartmentSearchService {
    private static final int[] AUTO_RADII = {500, 1000, 3000};
    private static final int DEFAULT_LIMIT = 10;
    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private final ApartmentLocationRepository repository;

    public NearbyApartmentSearchService(ApartmentLocationRepository repository) {
        this.repository = repository;
    }

    public NearbyApartmentResponse search(NearbyApartmentRequest request) {
        validateCoordinates(request.latitude(), request.longitude());
        int limit = request.limit() == null ? DEFAULT_LIMIT : request.limit();
        if (limit < 1 || limit > 50) throw new IllegalArgumentException("조회 개수는 1~50개여야 합니다.");
        if (!repository.isAvailable()) {
            return new NearbyApartmentResponse("DATASET_UNAVAILABLE",
                    "아파트 위치 데이터셋에 연결할 수 없습니다.", 0,
                    repository.datasetLocation(), List.of());
        }

        int[] radii = request.radiusMeters() == null ? AUTO_RADII : new int[]{request.radiusMeters()};
        for (int radius : radii) {
            if (radius < 1 || radius > 10000) throw new IllegalArgumentException("검색 반경은 1~10000m여야 합니다.");
            List<NearbyApartmentResponse.ApartmentCandidate> apartments = repository
                    .findCandidates(request.latitude(), request.longitude(), radius).stream()
                    .filter(this::hasValidCoordinates)
                    .map(item -> toCandidate(item, request.latitude(), request.longitude()))
                    .filter(item -> item.distanceMeters() <= radius)
                    .sorted(Comparator.comparingLong(NearbyApartmentResponse.ApartmentCandidate::distanceMeters)
                            .thenComparing(NearbyApartmentResponse.ApartmentCandidate::apartmentName))
                    .limit(limit).toList();
            if (!apartments.isEmpty()) {
                return new NearbyApartmentResponse("SUCCESS", null, radius,
                        repository.datasetLocation(), apartments);
            }
        }
        int finalRadius = radii[radii.length - 1];
        return new NearbyApartmentResponse("NOT_FOUND",
                finalRadius + "m 이내에서 아파트를 찾을 수 없습니다.", finalRadius,
                repository.datasetLocation(), List.of());
    }

    private NearbyApartmentResponse.ApartmentCandidate toCandidate(ApartmentLocation item,
                                                                    double latitude, double longitude) {
        long distance = Math.round(distanceMeters(latitude, longitude, item.latitude(), item.longitude()));
        return new NearbyApartmentResponse.ApartmentCandidate(item.apartmentId(), item.apartmentName(),
                item.address(), item.sggCode(), item.dongCode(), item.latitude(), item.longitude(), distance,
                item.averageTradeAmount(), item.averagePyeongAmount(), item.dealCount(),
                item.latestDealDate(), item.baseDate());
    }

    static double distanceMeters(double latitudeA, double longitudeA, double latitudeB, double longitudeB) {
        double latDelta = Math.toRadians(latitudeB - latitudeA);
        double lonDelta = Math.toRadians(longitudeB - longitudeA);
        double a = Math.sin(latDelta / 2) * Math.sin(latDelta / 2)
                + Math.cos(Math.toRadians(latitudeA)) * Math.cos(Math.toRadians(latitudeB))
                * Math.sin(lonDelta / 2) * Math.sin(lonDelta / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private boolean hasValidCoordinates(ApartmentLocation item) {
        return item.latitude() != null && item.longitude() != null
                && Double.isFinite(item.latitude()) && Double.isFinite(item.longitude())
                && item.latitude() >= -90 && item.latitude() <= 90
                && item.longitude() >= -180 && item.longitude() <= 180;
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("기준 장소의 위도 또는 경도가 올바르지 않습니다.");
        }
    }
}
