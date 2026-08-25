package com.seoul.market.seoulmarketprice.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QuestionAnalysisServiceTest {
    @Test
    void callsFastApiAndReadsNearestApartmentToolPlan() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("http://ai.test/ai/analyze-question"))
                .andExpect(content().json("{\"question\":\"홍대입구역에서 가장 가까운 아파트의 가격 알려줘\"}"))
                .andRespond(withSuccess("""
                        {
                          "intent":"NEAREST_APARTMENT_PRICE",
                          "regions":[],
                          "referencePlace":{"name":"홍대입구역","type":"STATION"},
                          "target":"APARTMENT",
                          "metric":"AVERAGE_PRICE",
                          "direction":"ASC",
                          "limit":1,
                          "period":null,
                          "requestedMetrics":["LATEST_PRICE"],
                          "toolPlan":["RESOLVE_PLACE","SEARCH_NEARBY_APARTMENTS","CALCULATE_DISTANCE","GET_APARTMENT_PRICE"],
                          "missingFields":[]
                        }
                        """, MediaType.APPLICATION_JSON));

        QuestionAnalysisService service = new QuestionAnalysisService(builder.build());
        var result = service.analyze("홍대입구역에서 가장 가까운 아파트의 가격 알려줘");

        assertEquals("NEAREST_APARTMENT_PRICE", result.intent());
        assertEquals("홍대입구역", result.referencePlace().name());
        assertEquals(4, result.toolPlan().size());
        server.verify();
    }
}
