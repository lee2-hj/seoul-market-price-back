package com.seoul.market.seoulmarketprice.ai.query;

import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocation;
import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocationRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/** Adapts factual apartment rows from the MinIO Parquet repository to the common metric contract. */
@Component
public class ParquetApartmentMetricDataSourceAdapter implements DataSourceAdapter {
    private static final double SQUARE_METERS_PER_PYEONG = 3.305785D;
    private final ApartmentLocationRepository apartmentLocationRepository;

    public ParquetApartmentMetricDataSourceAdapter(ApartmentLocationRepository apartmentLocationRepository) {
        this.apartmentLocationRepository = apartmentLocationRepository;
    }

    public boolean isAvailable() {
        return apartmentLocationRepository.isAvailable();
    }

    @Override
    public boolean supports(SearchScope scope) {
        return scope.type() == SearchScope.Type.DISTRICT || scope.type() == SearchScope.Type.DONG
                || scope.type() == SearchScope.Type.APARTMENT;
    }

    @Override
    public List<MetricRecord> fetch(SearchScope scope) {
        if (!apartmentLocationRepository.isAvailable()) {
            throw new IllegalStateException("아파트 Parquet 데이터셋에 연결할 수 없습니다.");
        }
        List<ApartmentLocation> locations = switch (scope.type()) {
            case DISTRICT, DONG -> findByRegion(null, null, scope.districtName(), scope.dongName());
            case APARTMENT -> apartmentLocationRepository.findByApartmentNameLike(scope.apartmentName());
            default -> List.of();
        };
        return toMetricRecords(locations);
    }

    /** Code lookup is preferred; names are a resilient fallback for Parquet partitions with changed codes. */
    public List<MetricRecord> fetchByRegion(String districtCode, String dongCode,
                                            String districtName, String dongName) {
        if (!apartmentLocationRepository.isAvailable()) {
            throw new IllegalStateException("아파트 Parquet 데이터셋에 연결할 수 없습니다.");
        }
        return toMetricRecords(findByRegion(districtCode, dongCode, districtName, dongName));
    }

    private List<ApartmentLocation> findByRegion(String districtCode, String dongCode,
                                                  String districtName, String dongName) {
        if (districtCode != null) {
            List<ApartmentLocation> byCode = apartmentLocationRepository.findByRegion(districtCode, dongCode);
            if (!byCode.isEmpty()) return byCode;
        }
        return apartmentLocationRepository.findByRegionName(districtName, dongName);
    }

    private List<MetricRecord> toMetricRecords(List<ApartmentLocation> locations) {
        return locations.stream()
                .filter(item -> item.averageTradeAmount() != null && item.dealCount() != null && item.dealCount() > 0)
                .map(this::toMetricRecord)
                .toList();
    }

    private MetricRecord toMetricRecord(ApartmentLocation item) {
        Double pyeong = item.exclusiveAreaM2() == null ? null : item.exclusiveAreaM2() / SQUARE_METERS_PER_PYEONG;
        Long averageTradeAmount = item.averageTradeAmount();
        return new MetricRecord(item.apartmentId() + "|" + item.exclusiveAreaM2(), item.districtName(), item.dongName(),
                item.apartmentName(), item.address(), averageTradeAmount == null ? null : averageTradeAmount * 10_000L,
                item.averagePyeongAmount(), item.exclusiveAreaM2(), pyeong,
                item.dealCount() == null ? null : item.dealCount().longValue(),
                item.latestDealDate(), item.baseDate());
    }
}
