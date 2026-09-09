package com.seoul.market.seoulmarketprice.ai.controller;

import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminAiDatasetControllerTest {
    @Test
    void refreshesApartmentDatasetAndReturnsActiveSnapshot() throws Exception {
        ApartmentLocationRepository repository = mock(ApartmentLocationRepository.class);
        when(repository.refresh()).thenReturn(new ApartmentLocationRepository.DatasetRefreshResult(
                "warehouse/mart/dm_main/base_date=2026-09-04/", 3453, List.of()));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminAiDatasetController(repository)).build();

        mockMvc.perform(post("/api/admin/ai/datasets/apartment-main/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasetLocation").value("warehouse/mart/dm_main/base_date=2026-09-04/"))
                .andExpect(jsonPath("$.rowCount").value(3453))
                .andExpect(jsonPath("$.dataQualityWarnings").isEmpty());
    }
}
