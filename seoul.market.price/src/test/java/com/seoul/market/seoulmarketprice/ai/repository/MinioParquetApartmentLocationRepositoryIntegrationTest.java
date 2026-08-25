package com.seoul.market.seoulmarketprice.ai.repository;

import com.seoul.market.seoulmarketprice.ai.config.ApartmentDatasetProperties;
import com.seoul.market.seoulmarketprice.ai.dto.NearbyApartmentRequest;
import com.seoul.market.seoulmarketprice.ai.dto.NearbyApartmentResponse;
import com.seoul.market.seoulmarketprice.ai.service.NearbyApartmentSearchService;
import com.seoul.market.seoulmarketprice.ai.service.KakaoPlaceResolver;
import com.seoul.market.seoulmarketprice.ai.service.NearestApartmentPriceSearchService;
import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import com.seoul.market.seoulmarketprice.config.KakaoProperties;
import com.seoul.market.seoulmarketprice.location.client.KakaoPlaceClient;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MinioParquetApartmentLocationRepositoryIntegrationTest {

    @Test
    void readsLatestPartitionAndFindsNearbyApartment() {
        String endpoint = System.getenv("MINIO_ENDPOINT");
        String accessKey = System.getenv("MINIO_ACCESS_KEY");
        String secretKey = System.getenv("MINIO_SECRET_KEY");
        assumeTrue(hasText(endpoint) && hasText(accessKey) && hasText(secretKey));

        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        ApartmentDatasetProperties properties = new ApartmentDatasetProperties(
                "minio", "MINIO_PARQUET", "warehouse", "mart/dm_main", 3600L);
        MinioParquetApartmentLocationRepository repository =
                new MinioParquetApartmentLocationRepository(client, properties);
        NearbyApartmentSearchService service = new NearbyApartmentSearchService(repository);

        NearbyApartmentResponse response = service.search(
                new NearbyApartmentRequest(37.521229, 127.109983, 500, 10));

        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.dataset()).contains("base_date=");
        assertThat(response.apartments()).isNotEmpty();
        assertThat(response.apartments().getFirst().distanceMeters()).isLessThanOrEqualTo(500);
        assertThat(response.apartments().getFirst().averageTradeAmount()).isPositive();
        assertThat(response.apartments().getFirst().dealCount()).isPositive();
        assertThat(response.apartments()).isSortedAccordingTo(
                java.util.Comparator.comparingLong(NearbyApartmentResponse.ApartmentCandidate::distanceMeters));
    }

    @Test
    void resolvesHongdaeStationAndReturnsActualParquetPrice() {
        String endpoint = System.getenv("MINIO_ENDPOINT");
        String accessKey = System.getenv("MINIO_ACCESS_KEY");
        String secretKey = System.getenv("MINIO_SECRET_KEY");
        String kakaoKey = System.getenv("KAKAO_REST_API_KEY");
        assumeTrue(hasText(endpoint) && hasText(accessKey) && hasText(secretKey) && hasText(kakaoKey));
        MinioClient client = MinioClient.builder().endpoint(endpoint)
                .credentials(accessKey, secretKey).build();
        var repository = new MinioParquetApartmentLocationRepository(client,
                new ApartmentDatasetProperties("minio", "MINIO_PARQUET", "warehouse", "mart/dm_main", 3600L));
        var nearby = new NearbyApartmentSearchService(repository);
        var placeResolver = new KakaoPlaceResolver(new KakaoPlaceClient(new KakaoProperties(kakaoKey)));
        var service = new NearestApartmentPriceSearchService(placeResolver, nearby);
        var analysis = new QuestionAnalysisResponse("NEAREST_APARTMENT_PRICE", java.util.List.of(),
                new QuestionAnalysisResponse.AnalyzedPlace("홍대입구", "STATION"), "APARTMENT",
                null, null, 1, null, java.util.List.of("LATEST_PRICE"), java.util.List.of(), java.util.List.of());

        var response = service.search(analysis);

        assertThat(response.summary()).contains("가장 가까운 아파트", "평균 거래가는");
        assertThat(response.keyPoints()).anyMatch(value -> value.startsWith("거리: 약 "));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
