package com.seoul.market.seoulmarketprice.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QuestionAnalysisServiceTest {
    @Test
    void marksMissingFieldsWhenRankingPlanIsIncomplete() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("http://ai.test/ai/analyze-question"))
                .andRespond(withSuccess("""
                        {"intent":"APARTMENT_RANKING", "regions":[], "metric":null}
                        """, MediaType.APPLICATION_JSON));

        var result = new QuestionAnalysisService(builder.build()).analyze("비싼 아파트 알려줘");

        assertTrue(result.requiresClarification());
        assertTrue(result.missingFields().contains("region"));
        assertTrue(result.missingFields().contains("metric"));
        server.verify();
    }

    @Test
    void normalizesUnsafeModelOutputBeforeReturningIt() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("http://ai.test/ai/analyze-question"))
                .andRespond(withSuccess("""
                        {
                          "intent":" APARTMENT_RANKING ", "limit":999,
                          "metricCandidates":[{"metric":" trade_count ","confidence":4.2,"reason":" ranking "}, null],
                          "missingFields":["", "metric"]
                        }
                        """, MediaType.APPLICATION_JSON));

        var result = new QuestionAnalysisService(builder.build()).analyze("  거래량 많은 곳  ");

        assertEquals("APARTMENT_RANKING", result.intent());
        assertEquals(100, result.limit());
        assertTrue(result.requiresClarification());
        assertEquals(java.util.List.of("metric", "limit", "region"), result.missingFields());
        assertEquals("TRADE_COUNT", result.metricCandidates().getFirst().metric());
        assertEquals(1.0, result.metricCandidates().getFirst().confidence());
        server.verify();
    }
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
