package com.seoul.market.seoulmarketprice.ai.repository;

import com.seoul.market.seoulmarketprice.ai.config.ApartmentDatasetProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;

/** MinIO 스키마 확인 전 운영에서 Fake 데이터를 반환하지 않기 위한 안전한 기본 구현이다. */
@Repository
@ConditionalOnProperty(name = "app.datasets.apartment-main.mode", havingValue = "disabled", matchIfMissing = true)
public class DisabledApartmentLocationRepository implements ApartmentLocationRepository {
    private final ApartmentDatasetProperties properties;

    public DisabledApartmentLocationRepository(ApartmentDatasetProperties properties) {
        this.properties = properties;
    }

    @Override public boolean isAvailable() { return false; }
    @Override public String datasetLocation() { return properties.location(); }
    @Override public List<ApartmentLocation> findCandidates(double latitude, double longitude, int radiusMeters) {
        return List.of();
    }
}
