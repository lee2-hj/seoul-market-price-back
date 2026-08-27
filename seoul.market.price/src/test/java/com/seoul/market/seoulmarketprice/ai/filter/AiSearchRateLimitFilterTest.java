package com.seoul.market.seoulmarketprice.ai.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AiSearchRateLimitFilterTest {

    @Test
    void allowsFiveRequestsThenReturns429ForTheSameIp() throws Exception {
        AiSearchRateLimitFilter filter = new AiSearchRateLimitFilter(new ObjectMapper(), 5, 1);

        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = execute(filter, "/api/ai/search-natural", "203.0.113.10");
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse response = execute(filter, "/api/ai/search-natural", "203.0.113.10");

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentAsString()).contains("AI-RATE-001");
    }

    @Test
    void ignoresOtherAiEndpoints() throws Exception {
        AiSearchRateLimitFilter filter = new AiSearchRateLimitFilter(new ObjectMapper(), 1, 1);

        MockHttpServletResponse response = execute(filter, "/api/ai/analyze-question", "203.0.113.10");

        assertThat(response.getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse execute(
            AiSearchRateLimitFilter filter,
            String uri,
            String remoteAddress
    ) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr(remoteAddress);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
