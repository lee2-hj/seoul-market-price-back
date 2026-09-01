package com.seoul.market.seoulmarketprice.ai.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seoul.market.seoulmarketprice.ai.dto.PriceRankingResponse;
import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import com.seoul.market.seoulmarketprice.ai.dto.RankingCriteria;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.List;

/** Bridges the FastAPI apartment-data RAG endpoint to the existing search response contract. */
@Service
public class ApartmentDataRagSearchService {
    private final RestClient aiClient;
    private final RankingRegionResolver regionResolver;

    public ApartmentDataRagSearchService(@Qualifier("aiFastApiRestClient") RestClient aiClient,
                                         SggMasterRepository sggRepository,
                                         LocationMasterService locationService) {
        this.aiClient = aiClient;
        this.regionResolver = new RankingRegionResolver(sggRepository, locationService);
    }

    public PriceRankingResponse search(String question, QuestionAnalysisResponse analysis) {
        RankingRegionResolver.ResolvedRegion region = regionResolver.resolve(question);
        String[] names = region.allSeoul() ? new String[0] : region.name().split("\\s+", 2);
        QuestionAnalysisResponse.SearchFilters filters = analysis.filters();
        ApartmentRagResponse response = aiClient.post().uri("/ai/apartment-rag/search")
                .body(new ApartmentRagRequest(question,
                        names.length == 0 ? null : names[0], names.length < 2 ? null : names[1],
                        filters == null ? null : filters.minPyeong(), filters == null ? null : filters.maxPyeong(),
                        filters == null ? null : filters.minPriceWon(), filters == null ? null : filters.maxPriceWon(),
                        analysis.limit() == null ? 5 : Math.min(analysis.limit(), 30)))
                .retrieve().body(ApartmentRagResponse.class);
        List<ApartmentRagCandidate> candidates = response == null || response.candidates() == null ? List.of() : response.candidates();
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("요청 조건에 맞는 아파트 거래 데이터를 찾을 수 없습니다.");
        }
        List<PriceRankingResponse.Item> items = candidates.stream()
                .sorted(Comparator.comparing(ApartmentRagCandidate::averageTradeAmountManwon,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ApartmentRagCandidate::pyeong, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ApartmentRagCandidate::dealCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(analysis.limit() == null ? 5 : analysis.limit())
                .map(item -> new PriceRankingResponse.Item(0, item.district() + " " + item.dong(),
                        item.apartmentName(), item.averageTradeAmountManwon(), item.dealCount() == null ? 0 : item.dealCount(),
                        item.exclusiveAreaM2(), item.pyeong(), item.dealDate()))
                .toList();
        List<PriceRankingResponse.Item> unrankedItems = items;
        items = java.util.stream.IntStream.range(0, unrankedItems.size())
                .mapToObj(index -> new PriceRankingResponse.Item(index + 1, unrankedItems.get(index).regionName(),
                        unrankedItems.get(index).apartmentName(), unrankedItems.get(index).metricValue(), unrankedItems.get(index).dealCount(),
                        unrankedItems.get(index).exclusiveAreaM2(), unrankedItems.get(index).pyeong(), unrankedItems.get(index).dealDate()))
                .toList();
        String baseDate = candidates.stream().map(ApartmentRagCandidate::baseDate).filter(value -> value != null && !value.isBlank())
                .max(String::compareTo).orElse(null);
        String label = region.allSeoul() ? "서울 전체" : region.name();
        return new PriceRankingResponse(label + " 아파트 검색 결과", "thing_amt", baseDate,
                new RankingCriteria("평균 거래가 · 전용면적 · 거래량", "만원 · ㎡ · 건", baseDate == null ? "최신 집계 기간" : baseDate + " 기준", 0, "가격·평수·거래량 높은 순"),
                items, response.groundedAnswer());
    }

    private record ApartmentRagRequest(String question, String district, String dong,
                                       @JsonProperty("min_pyeong") Double minPyeong,
                                       @JsonProperty("max_pyeong") Double maxPyeong,
                                       @JsonProperty("min_price_won") Long minPriceWon,
                                       @JsonProperty("max_price_won") Long maxPriceWon,
                                       Integer limit) {}
    private record ApartmentRagResponse(List<ApartmentRagCandidate> candidates, String groundedAnswer) {}
    private record ApartmentRagCandidate(String apartmentName, String district, String dong,
                                         Long averageTradeAmountManwon, Integer dealCount,
                                         String baseDate, Double score, Double exclusiveAreaM2,
                                         Double pyeong, String dealDate) {}
}
