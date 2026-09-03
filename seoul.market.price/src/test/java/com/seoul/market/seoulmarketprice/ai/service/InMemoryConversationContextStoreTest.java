package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryConversationContextStoreTest {
    @Test
    void expiresEntriesOnReadWithoutBackgroundScheduler() {
        var store = new InMemoryConversationContextStore(Duration.ofSeconds(-1));
        store.put("session", analysis());

        assertThat(store.get("session")).isEmpty();
    }

    @Test
    void evictsEntryExplicitly() {
        var store = new InMemoryConversationContextStore(Duration.ofMinutes(15));
        store.put("session", analysis());
        store.evict("session");

        assertThat(store.get("session")).isEmpty();
    }

    private QuestionAnalysisResponse analysis() {
        return new QuestionAnalysisResponse("SINGLE_REGION", List.of(), null, "REGION", null,
                "AVERAGE_PRICE", null, 5, null, List.of(), List.of(), List.of(), null, List.of(),
                new QuestionAnalysisResponse.SearchFilters(null, null, null, null), false);
    }
}
