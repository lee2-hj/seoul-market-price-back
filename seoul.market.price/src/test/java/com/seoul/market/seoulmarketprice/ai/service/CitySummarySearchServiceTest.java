package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.SingleRegionPriceRequest;
import com.seoul.market.seoulmarketprice.ai.dto.SingleRegionPriceResponse;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.ListResponse;
import com.seoul.market.seoulmarketprice.fastapi.service.FastApiService;
import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CitySummarySearchServiceTest {
    @Test
    void calculatesTradeCountWeightedCityAverageBeforeRequestingExplanation() {
        SggMasterRepository repository = mock(SggMasterRepository.class);
        FastApiService fastApi = mock(FastApiService.class);
        RestClient client = mock(RestClient.class);
        RestClient.RequestBodyUriSpec request = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodyRequest = mock(RestClient.RequestBodySpec.class);
        SggMaster first = district("11110", "종로구");
        SggMaster second = district("11140", "중구");
        when(repository.findAllByOrderBySggNameAsc()).thenReturn(List.of(first, second));
        when(fastApi.getPyeongList(any())).thenReturn(
                response("2026-09-01", 2, 100_000L, 1_000L),
                response("2026-09-02", 1, 200_000L, 2_000L));
        when(client.post()).thenReturn(request);
        when(request.uri(anyString())).thenReturn(bodyRequest);

        SingleRegionPriceResponse result = new CitySummarySearchService(repository, fastApi, client)
                .search("서울시 아파트 전체 평균가격 알려줘");

        ArgumentCaptor<SingleRegionPriceRequest> requestCaptor = ArgumentCaptor.forClass(SingleRegionPriceRequest.class);
        org.mockito.Mockito.verify(bodyRequest).body(requestCaptor.capture());
        assertThat(requestCaptor.getValue().facts().averagePrice()).isEqualTo(133_333L);
        assertThat(requestCaptor.getValue().facts().averagePyeongPrice()).isEqualTo(1_333L);
        assertThat(requestCaptor.getValue().facts().transactionCount()).isEqualTo(3);
        assertThat(requestCaptor.getValue().facts().baseDate()).isEqualTo("2026-09-02");
        assertThat(result.summary()).isEqualTo("서울시 전체 평균 거래가는 13억 3,333만원입니다.");
    }

    @Test
    void returnsFactualFallbackWhenExplanationServiceFails() {
        SggMasterRepository repository = mock(SggMasterRepository.class);
        FastApiService fastApi = mock(FastApiService.class);
        RestClient client = mock(RestClient.class);
        RestClient.RequestBodyUriSpec request = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodyRequest = mock(RestClient.RequestBodySpec.class);
        SggMaster district = district("11110", "종로구");
        when(repository.findAllByOrderBySggNameAsc()).thenReturn(List.of(district));
        when(fastApi.getPyeongList(any())).thenReturn(response("2026-09-02", 2, 125_000L, 1_500L));
        when(client.post()).thenReturn(request);
        when(request.uri(anyString())).thenReturn(bodyRequest);

        SingleRegionPriceResponse result = new CitySummarySearchService(repository, fastApi, client)
                .search("서울시 아파트 전체 평균가격 알려줘");

        assertThat(result.summary()).isEqualTo("서울시 전체 평균 거래가는 12억 5,000만원입니다.");
        assertThat(result.keyPoints()).contains("평균 평단가: 1,500만원/평", "거래 건수: 2건");
        assertThat(result.cautions()).contains("기준일: 2026-09-02");
    }

    private SggMaster district(String code, String name) {
        SggMaster district = mock(SggMaster.class);
        when(district.getSggCode()).thenReturn(code);
        when(district.getSggName()).thenReturn(name);
        return district;
    }

    private ListResponse response(String baseDate, int count, long averagePrice, long averagePyeongPrice) {
        return new ListResponse(baseDate, Map.of("all", new ListResponse.ListSummaryDto(
                "all", "전체", count, averagePrice, averagePyeongPrice)));
    }
}
