package com.seoul.market.seoulmarketprice.ai.controller;

import com.seoul.market.seoulmarketprice.ai.dto.NaturalApartmentCandidate;
import com.seoul.market.seoulmarketprice.ai.dto.NaturalSearchErrorCode;
import com.seoul.market.seoulmarketprice.ai.dto.NaturalSearchResponse;
import com.seoul.market.seoulmarketprice.ai.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AiControllerHttpContractTest {
    private NaturalLanguageSearchService naturalLanguageSearchService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        naturalLanguageSearchService = mock(NaturalLanguageSearchService.class);
        AiController controller = new AiController(
                mock(AiExplanationService.class), naturalLanguageSearchService,
                mock(QuestionAnalysisService.class), mock(PlaceResolver.class),
                mock(NearbyApartmentSearchService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void returnsNoPriceDataContract() throws Exception {
        when(naturalLanguageSearchService.search(anyString(), isNull(), nullable(String.class)))
                .thenReturn(NaturalSearchResponse.error("가격 데이터가 없습니다.", NaturalSearchErrorCode.NO_PRICE_DATA));

        mockMvc.perform(post("/api/ai/search-natural")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"없는 단지 가격 알려줘\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("NO_PRICE_DATA"));
    }

    @Test
    void promotesSuccessToPartialDataWhenQualityWarningsExist() throws Exception {
        NaturalSearchResponse response = NaturalSearchResponse.success("APARTMENT_DETAIL", "result")
                .withDataQualityWarnings(List.of("일부 위치는 근사 좌표입니다."));
        when(naturalLanguageSearchService.search(anyString(), isNull(), nullable(String.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/ai/search-natural")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"단지 가격 알려줘\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIAL_DATA"))
                .andExpect(jsonPath("$.dataQualityWarnings[0]").value("일부 위치는 근사 좌표입니다."));
    }

    @Test
    void returnsSeparateCandidatesForSameNameDifferentLots() throws Exception {
        NaturalSearchResponse response = NaturalSearchResponse.apartmentClarification(
                "단지를 선택해주세요.",
                List.of(
                        new NaturalApartmentCandidate("남광아파트", "강서구", "11500", "화곡동", "10300", "0022", "0001"),
                        new NaturalApartmentCandidate("남광아파트", "강서구", "11500", "화곡동", "10300", "0042", "0000")));
        when(naturalLanguageSearchService.search(anyString(), isNull(), nullable(String.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/ai/search-natural")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"화곡동 남광아파트 가격 알려줘\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEED_CLARIFICATION"))
                .andExpect(jsonPath("$.apartmentCandidates.length()").value(2))
                .andExpect(jsonPath("$.apartmentCandidates[0].mno").value("0022"))
                .andExpect(jsonPath("$.apartmentCandidates[1].mno").value("0042"));
    }
}
