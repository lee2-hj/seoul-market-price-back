package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.config.PriceAnomalyProperties;
import com.seoul.market.seoulmarketprice.fastapi.dto.request.ListRequest;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.ListResponse;
import com.seoul.market.seoulmarketprice.fastapi.service.FastApiService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FastApiRegionPriceBaselineProvider implements RegionPriceBaselineProvider {
    private final FastApiService fastApiService;
    private final Duration ttl;
    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();

    public FastApiRegionPriceBaselineProvider(FastApiService fastApiService, PriceAnomalyProperties properties) {
        this.fastApiService = fastApiService;
        this.ttl = Duration.ofMinutes(properties.cacheTtlMinutes());
    }

    @Override
    public Optional<Baseline> baseline(String sggCode) {
        if (sggCode == null || sggCode.isBlank()) return Optional.empty();
        Entry cached = cache.get(sggCode);
        if (cached != null && cached.createdAt().plus(ttl).isAfter(Instant.now())) return Optional.of(cached.baseline());
        try {
            ListResponse response = fastApiService.getPyeongList(new ListRequest(sggCode));
            long weightedSum = 0, count = 0;
            if (response == null || response.groups() == null) return Optional.empty();
            for (ListResponse.ListSummaryDto item : response.groups().values()) {
                long deals = item.total_count() == null ? 0 : item.total_count();
                if (deals <= 0 || item.avg_thing_amt() == null) continue;
                weightedSum += item.avg_thing_amt() * deals;
                count += deals;
            }
            if (count == 0) return Optional.empty();
            Baseline baseline = new Baseline(Math.round((double) weightedSum / count) * 10_000L, Math.toIntExact(count));
            cache.put(sggCode, new Entry(baseline, Instant.now()));
            return Optional.of(baseline);
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }
    private record Entry(Baseline baseline, Instant createdAt) {}
}
