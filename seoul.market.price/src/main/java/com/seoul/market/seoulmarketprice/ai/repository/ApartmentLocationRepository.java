package com.seoul.market.seoulmarketprice.ai.repository;

import java.util.List;

/** 실제 구현은 Parquet에서 좌표 bounding box 조건을 먼저 적용해야 한다. */
public interface ApartmentLocationRepository {
    boolean isAvailable();
    String datasetLocation();
    List<ApartmentLocation> findCandidates(double latitude, double longitude, int radiusMeters);
    default List<ApartmentLocation> findByRegion(String sggCode, String dongCode) {
        return List.of();
    }
}
