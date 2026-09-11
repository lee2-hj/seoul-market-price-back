package com.seoul.market.seoulmarketprice.elasticSearch.service;

import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocation;
import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocationRepository;
import com.seoul.market.seoulmarketprice.elasticSearch.dto.response.AptNameResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * apt_name 인덱스는 dm_main 최신 Parquet 파티션 대비 갱신이 지연될 수 있어 mno/sno가 실제 값과
 * 어긋나는 사례(예: "강동리버스트8단지" ES mno=0066 vs 실제 mno=0701)가 보고되어,
 * ElasticSearchService.reconcileWithLatestPartition()이 이를 교정하는지 검증한다.
 */
class ElasticSearchServiceTest {

    private static final AptNameResponse STALE_HIT = new AptNameResponse(
            "강동리버스트8단지", "0066", "0000", "11000", "강일동", "11740", "강동구");

    @Test
    void correctsMnoSnoWhenExactlyOneParquetMatchExists() {
        FakeRepository repository = new FakeRepository(true, List.of(
                parquetLocation("강동리버스트8단지", "11740", "11000", "0701", "0000")));
        ElasticSearchService service = new ElasticSearchService(null, repository);

        List<AptNameResponse> result = service.reconcileWithLatestPartition(List.of(STALE_HIT));

        AptNameResponse corrected = result.get(0);
        assertThat(corrected.mno()).isEqualTo("0701");
        assertThat(corrected.sno()).isEqualTo("0000");
        assertThat(corrected.apt_name()).isEqualTo("강동리버스트8단지");
        assertThat(corrected.dong_cd()).isEqualTo("11000");
        assertThat(corrected.sgg_cd()).isEqualTo("11740");
    }

    @Test
    void keepsEsValueWhenNoParquetMatchExists() {
        FakeRepository repository = new FakeRepository(true, List.of());
        ElasticSearchService service = new ElasticSearchService(null, repository);

        List<AptNameResponse> result = service.reconcileWithLatestPartition(List.of(STALE_HIT));

        assertThat(result.get(0)).isSameAs(STALE_HIT);
    }

    @Test
    void keepsEsValueWhenMultipleParquetMatchesAreAmbiguous() {
        FakeRepository repository = new FakeRepository(true, List.of(
                parquetLocation("강동리버스트8단지", "11740", "11000", "0701", "0000"),
                parquetLocation("강동리버스트8단지", "11740", "11000", "0702", "0001")));
        ElasticSearchService service = new ElasticSearchService(null, repository);

        List<AptNameResponse> result = service.reconcileWithLatestPartition(List.of(STALE_HIT));

        assertThat(result.get(0)).isSameAs(STALE_HIT);
    }

    @Test
    void keepsEsValueWhenRepositoryIsUnavailable() {
        FakeRepository repository = new FakeRepository(false, List.of(
                parquetLocation("강동리버스트8단지", "11740", "11000", "0701", "0000")));
        ElasticSearchService service = new ElasticSearchService(null, repository);

        List<AptNameResponse> result = service.reconcileWithLatestPartition(List.of(STALE_HIT));

        assertThat(result.get(0)).isSameAs(STALE_HIT);
        assertThat(repository.regionCalls).isZero();
    }

    @Test
    void leavesAlreadyCorrectHitUnchanged() {
        AptNameResponse freshHit = new AptNameResponse(
                "강동리버스트8단지", "0701", "0000", "11000", "강일동", "11740", "강동구");
        FakeRepository repository = new FakeRepository(true, List.of(
                parquetLocation("강동리버스트8단지", "11740", "11000", "0701", "0000")));
        ElasticSearchService service = new ElasticSearchService(null, repository);

        List<AptNameResponse> result = service.reconcileWithLatestPartition(List.of(freshHit));

        assertThat(result.get(0)).isSameAs(freshHit);
    }

    private ApartmentLocation parquetLocation(String apartmentName, String sggCode, String dongCode,
                                               String mno, String sno) {
        String apartmentId = String.join("-", sggCode, dongCode, mno, sno);
        return new ApartmentLocation(apartmentId, apartmentName, "", sggCode, dongCode, 37.0, 127.0);
    }

    private static class FakeRepository implements ApartmentLocationRepository {
        private final boolean available;
        private final List<ApartmentLocation> locations;
        private int regionCalls;

        private FakeRepository(boolean available, List<ApartmentLocation> locations) {
            this.available = available;
            this.locations = locations;
        }

        @Override public boolean isAvailable() { return available; }
        @Override public String datasetLocation() { return "s3://warehouse/mart/dm_main/"; }
        @Override public List<ApartmentLocation> findCandidates(double latitude, double longitude, int radiusMeters) {
            return List.of();
        }
        @Override public List<ApartmentLocation> findByRegion(String sggCode, String dongCode) {
            regionCalls++;
            return locations.stream()
                    .filter(item -> sggCode.equals(item.sggCode()))
                    .filter(item -> dongCode == null || dongCode.equals(item.dongCode()))
                    .toList();
        }
    }
}
