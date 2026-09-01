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

    /**
     * Finds a region by the place names stored in Parquet.  This is the safety net for
     * location-master code revisions and source-data code mismatches.
     */
    default List<ApartmentLocation> findByRegionName(String districtName, String dongName) {
        return List.of();
    }

    default List<ApartmentLocation> findByApartmentNameLike(String apartmentName) {
        return List.of();
    }

    default RegionLookup findByRegionWithFallback(String sggCode, String dongCode,
                                                   String districtName, String dongName) {
        List<ApartmentLocation> exact = findByRegion(sggCode, dongCode);
        if (!exact.isEmpty()) return new RegionLookup(exact, "CODE_EXACT");
        List<ApartmentLocation> byName = findByRegionName(districtName, dongName);
        return new RegionLookup(byName, byName.isEmpty() ? "NOT_FOUND" : "PARQUET_NAME_FALLBACK");
    }

    record RegionLookup(List<ApartmentLocation> locations, String strategy) {}
}
