package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.NearbyApartmentRequest;
import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocation;
import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocationRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NearbyApartmentSearchServiceTest {
    private static final double HONGDAE_LAT = 37.557192;
    private static final double HONGDAE_LON = 126.925381;

    @Test
    void sortsByDistanceAndExcludesInvalidOrOutsideRadiusCandidates() {
        FakeRepository repository = new FakeRepository(true, List.of(
                apartment("far", "먼 아파트", HONGDAE_LAT + 0.004, HONGDAE_LON),
                apartment("near", "가까운 아파트", HONGDAE_LAT + 0.001, HONGDAE_LON),
                apartment("outside", "반경 밖 아파트", HONGDAE_LAT + 0.02, HONGDAE_LON),
                new ApartmentLocation("invalid", "좌표 없는 아파트", "", "11440", "", null, null)
        ));
        NearbyApartmentSearchService service = new NearbyApartmentSearchService(repository);

        var result = service.search(new NearbyApartmentRequest(HONGDAE_LAT, HONGDAE_LON, 500, 10));

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.radiusMeters()).isEqualTo(500);
        assertThat(result.apartments()).extracting(item -> item.apartmentId())
                .containsExactly("near", "far");
        assertThat(result.apartments().get(0).distanceMeters()).isLessThan(result.apartments().get(1).distanceMeters());
    }

    @Test
    void expandsRadiusFromFiveHundredMetersUntilCandidateExists() {
        ExpandingRepository repository = new ExpandingRepository();
        NearbyApartmentSearchService service = new NearbyApartmentSearchService(repository);

        var result = service.search(new NearbyApartmentRequest(HONGDAE_LAT, HONGDAE_LON, null, 1));

        assertThat(repository.requestedRadii).containsExactly(500, 1000);
        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.radiusMeters()).isEqualTo(1000);
        assertThat(result.apartments()).hasSize(1);
    }

    @Test
    void reportsConfiguredDatasetWhenMinioIsUnavailable() {
        FakeRepository repository = new FakeRepository(false, List.of());
        var result = new NearbyApartmentSearchService(repository)
                .search(new NearbyApartmentRequest(HONGDAE_LAT, HONGDAE_LON, null, null));

        assertThat(result.status()).isEqualTo("DATASET_UNAVAILABLE");
        assertThat(result.dataset()).isEqualTo("s3://warehouse/mart/dm_main/");
        assertThat(repository.calls).isZero();
    }

    @Test
    void rejectsInvalidReferenceCoordinates() {
        NearbyApartmentSearchService service = new NearbyApartmentSearchService(
                new FakeRepository(true, List.of()));

        assertThatThrownBy(() -> service.search(new NearbyApartmentRequest(91, HONGDAE_LON, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("기준 장소의 위도 또는 경도가 올바르지 않습니다.");
    }

    @Test
    void haversineDistanceIsApproximatelyCorrect() {
        double distance = NearbyApartmentSearchService.distanceMeters(37.0, 127.0, 37.001, 127.0);
        assertThat(distance).isBetween(110.0, 112.0);
    }

    private ApartmentLocation apartment(String id, String name, double latitude, double longitude) {
        return new ApartmentLocation(id, name, "서울특별시 마포구", "11440", "1144012000",
                latitude, longitude);
    }

    private static class FakeRepository implements ApartmentLocationRepository {
        private final boolean available;
        private final List<ApartmentLocation> candidates;
        private int calls;

        private FakeRepository(boolean available, List<ApartmentLocation> candidates) {
            this.available = available;
            this.candidates = candidates;
        }

        @Override public boolean isAvailable() { return available; }
        @Override public String datasetLocation() { return "s3://warehouse/mart/dm_main/"; }
        @Override public List<ApartmentLocation> findCandidates(double latitude, double longitude, int radiusMeters) {
            calls++;
            return candidates;
        }
    }

    private class ExpandingRepository implements ApartmentLocationRepository {
        private final List<Integer> requestedRadii = new ArrayList<>();
        @Override public boolean isAvailable() { return true; }
        @Override public String datasetLocation() { return "s3://warehouse/mart/dm_main/"; }
        @Override public List<ApartmentLocation> findCandidates(double latitude, double longitude, int radiusMeters) {
            requestedRadii.add(radiusMeters);
            return radiusMeters < 1000 ? List.of()
                    : List.of(apartment("apt-1", "테스트 아파트", HONGDAE_LAT + 0.006, HONGDAE_LON));
        }
    }
}
