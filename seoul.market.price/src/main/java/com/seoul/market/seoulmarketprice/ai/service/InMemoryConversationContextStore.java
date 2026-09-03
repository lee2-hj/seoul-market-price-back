package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryConversationContextStore implements ConversationContextStore {
    private final ConcurrentHashMap<String, TimestampedEntry> entries = new ConcurrentHashMap<>();
    private final Duration ttl;

    @Autowired
    public InMemoryConversationContextStore(@Value("${ai.conversation-context.ttl-minutes:15}") long ttlMinutes) {
        this(Duration.ofMinutes(ttlMinutes));
    }

    InMemoryConversationContextStore(Duration ttl) {
        this.ttl = ttl;
    }

    @Override
    public Optional<QuestionAnalysisResponse> get(String sessionId) {
        TimestampedEntry entry = entries.get(sessionId);
        if (entry == null) return Optional.empty();
        if (entry.createdAt().plus(ttl).isAfter(Instant.now())) return Optional.of(entry.analysis());
        entries.remove(sessionId, entry);
        return Optional.empty();
    }

    @Override
    public void put(String sessionId, QuestionAnalysisResponse analysis) {
        entries.put(sessionId, new TimestampedEntry(analysis, Instant.now()));
    }

    @Override
    public void evict(String sessionId) {
        entries.remove(sessionId);
    }

    private record TimestampedEntry(QuestionAnalysisResponse analysis, Instant createdAt) {}
}
