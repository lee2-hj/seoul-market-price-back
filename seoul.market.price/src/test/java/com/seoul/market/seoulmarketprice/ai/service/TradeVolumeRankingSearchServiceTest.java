package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.fastapi.dto.response.RttRespopnse;
import com.seoul.market.seoulmarketprice.fastapi.service.FastApiService;
import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TradeVolumeRankingSearchServiceTest {
    @Test
    void returnsTopApartmentsByTradeVolumeInDistrict() {
        SggMasterRepository sggRepository = mock(SggMasterRepository.class);
        LocationMasterService locationService = mock(LocationMasterService.class);
        FastApiService fastApiService = mock(FastApiService.class);
        SggMaster sgg = mock(SggMaster.class);
        when(sgg.getSggCode()).thenReturn("11680");
        when(sgg.getSggName()).thenReturn("강남구");
        when(sggRepository.findBySggName("강남구")).thenReturn(Optional.of(sgg));
        when(fastApiService.getRttInfo(any())).thenReturn(response());

        RankingQuestionParser parser = new RankingQuestionParser(
                Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"), ZoneId.of("Asia/Seoul")));
        TradeVolumeRankingSearchService service = new TradeVolumeRankingSearchService(
                parser, sggRepository, locationService, fastApiService);

        var result = service.search("강남구에서 거래량이 많은 아파트 상위 2개 알려줘");

        assertEquals("강남구", result.regionName());
        assertEquals(2, result.items().size());
        assertEquals(1, result.items().get(0).rank());
        assertEquals("아파트 A", result.items().get(0).apartmentName());
        assertEquals(12, result.items().get(0).dealCount());
    }

    @Test
    void returnsTopApartmentsAcrossSeoulWhenUserExplicitlyRequestsIt() {
        SggMasterRepository sggRepository = mock(SggMasterRepository.class);
        LocationMasterService locationService = mock(LocationMasterService.class);
        FastApiService fastApiService = mock(FastApiService.class);
        when(locationService.getSggs()).thenReturn(List.of(
                new com.seoul.market.seoulmarketprice.location.dto.SggResponse("11110", "종로구"),
                new com.seoul.market.seoulmarketprice.location.dto.SggResponse("11680", "강남구")
        ));
        when(fastApiService.getRttInfo(any())).thenReturn(response());

        RankingQuestionParser parser = new RankingQuestionParser(
                Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"), ZoneId.of("Asia/Seoul")));
        TradeVolumeRankingSearchService service = new TradeVolumeRankingSearchService(
                parser, sggRepository, locationService, fastApiService);

        var result = service.search("서울 전체 거래량 상위 5개 아파트 알려줘");

        assertEquals("서울 전체", result.regionName());
        assertEquals("종로구", result.items().get(0).regionName());
        assertEquals(40, result.totalDealCount());
    }

    private RttRespopnse response() {
        return new RttRespopnse("11680", "강남구", null, null, "2026-08-01", "2026-08-24", 20,
                100L, 5L, 10L, 0.0, List.of(), List.of(), List.of(), List.of(
                new RttRespopnse.Top5ByVolumeDto("아파트 A", "1", "0", 12, 900_000_000L),
                new RttRespopnse.Top5ByVolumeDto("아파트 B", "2", "0", 8, 700_000_000L)
        ));
    }
}
