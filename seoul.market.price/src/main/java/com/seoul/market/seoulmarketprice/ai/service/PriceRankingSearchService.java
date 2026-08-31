package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.PriceRankingResponse;
import com.seoul.market.seoulmarketprice.ai.dto.RankingMetric;
import com.seoul.market.seoulmarketprice.ai.dto.RankingCriteria;
import com.seoul.market.seoulmarketprice.ai.dto.RankingSearchQuery;
import com.seoul.market.seoulmarketprice.ai.dto.SortDirection;
import com.seoul.market.seoulmarketprice.fastapi.dto.request.TopAndBottomRequest;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.TopAndBottomResponse;
import com.seoul.market.seoulmarketprice.fastapi.service.FastApiService;
import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocation;
import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocationRepository;
import com.seoul.market.seoulmarketprice.location.dto.DongRegionResponse;
import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

/** top-bottom API의 동별 후보를 집계해 가격 순위를 만든다. */
@Service
public class PriceRankingSearchService {
    private static final int FAST_API_MAX_LIMIT = 5;
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final RankingQuestionParser questionParser;
    private final SggMasterRepository sggRepository;
    private final LocationMasterService locationService;
    private final FastApiService fastApiService;
    private final ApartmentLocationRepository apartmentLocationRepository;
    private final RankingRegionResolver regionResolver;
    private final ConcurrentHashMap<CacheKey, CachedResponse> cache = new ConcurrentHashMap<>();

    public PriceRankingSearchService(RankingQuestionParser questionParser, SggMasterRepository sggRepository,
                                     LocationMasterService locationService, FastApiService fastApiService,
                                     ApartmentLocationRepository apartmentLocationRepository) {
        this.questionParser = questionParser;
        this.sggRepository = sggRepository;
        this.locationService = locationService;
        this.fastApiService = fastApiService;
        this.apartmentLocationRepository = apartmentLocationRepository;
        this.regionResolver = new RankingRegionResolver(sggRepository, locationService);
    }

    public PriceRankingResponse search(String question) {
        return search(question, questionParser.parse(question));
    }

    public PriceRankingResponse search(String question, RankingSearchQuery query) {
        if (query.metric() != RankingMetric.PRICE) {
            throw new IllegalArgumentException("가격 순위 질문만 처리할 수 있습니다.");
        }
        if (query.limit() > FAST_API_MAX_LIMIT) {
            throw new IllegalArgumentException("현재 가격 순위는 상위 5개까지만 제공됩니다.");
        }

        String metricType = pyeongMetric(question) ? "pyeong" : "thing_amt";
        Region region = resolveRegion(question);
        RankingQuestionParser.AreaRange areaRange = query.minPyeong() == null && query.maxPyeong() == null
                ? questionParser.areaRange(question)
                : new RankingQuestionParser.AreaRange(query.minPyeong(), query.maxPyeong());
        if (areaRange != null) {
            return searchByArea(region, areaRange, query);
        }
        List<Candidate> candidates = region == null
                ? allSeoulCandidates(metricType, query.direction())
                : regionCandidates(region, metricType, query.direction());
        return toResponse(region == null ? "서울 전체" : region.name(), metricType, candidates,
                query);
    }

    private PriceRankingResponse searchByArea(Region region, RankingQuestionParser.AreaRange areaRange,
                                               RankingSearchQuery query) {
        if (!apartmentLocationRepository.isAvailable()) {
            throw new IllegalArgumentException("전용면적 조건 검색용 Parquet 데이터셋에 연결할 수 없습니다.");
        }
        List<ApartmentLocation> source = region == null
                ? locationService.getSggs().stream().flatMap(sgg -> apartmentLocationRepository
                        .findByRegion(sgg.sggCd(), null).stream()).toList()
                : apartmentLocationRepository.findByRegion(region.sggCode(), region.dongCode());
        List<Candidate> candidates = source.stream()
                .filter(item -> item.exclusiveAreaM2() != null && item.averageTradeAmount() != null)
                .filter(item -> matchesArea(item.exclusiveAreaM2(), areaRange))
                .map(item -> new Candidate(item.address(), item.baseDate(), item.apartmentName(),
                        item.averageTradeAmount(), item.dealCount() == null ? 0 : item.dealCount()))
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("전용 " + formatPyeong(areaRange) + "평 조건의 아파트 거래 데이터를 찾을 수 없습니다.");
        }
        String regionName = (region == null ? "서울 전체" : region.name()) + " · 전용 "
                + formatPyeong(areaRange) + "평" + priceConditionLabel(query);
        return toResponse(regionName, "thing_amt", candidates, query);
    }

    private boolean matchesArea(double squareMeters, RankingQuestionParser.AreaRange range) {
        BigDecimal pyeong = BigDecimal.valueOf(squareMeters / 3.305785);
        return pyeong.compareTo(range.minimumPyeong()) >= 0 && pyeong.compareTo(range.maximumPyeong()) <= 0;
    }

    private String formatPyeong(RankingQuestionParser.AreaRange range) {
        return range.minimumPyeong().stripTrailingZeros().toPlainString() + "~"
                + range.maximumPyeong().stripTrailingZeros().toPlainString();
    }

    private String priceConditionLabel(RankingSearchQuery query) {
        if (query.minPrice() == null || query.maxPrice() == null) return "";
        return " · " + query.minPrice().divide(BigDecimal.valueOf(100_000_000)).stripTrailingZeros()
                .toPlainString() + "억대";
    }

    private List<Candidate> allSeoulCandidates(String metricType, SortDirection direction) {
        return locationService.getSggs().parallelStream()
                .flatMap(sgg -> locationService.getDongs(sgg.sggCd()).parallelStream()
                        .flatMap(dong -> regionCandidates(new Region(sgg.sggCd(), dong.dongCd(),
                                sgg.sggNm() + " " + dong.dongNm()), metricType, direction).stream()))
                .toList();
    }

    private List<Candidate> regionCandidates(Region region, String metricType, SortDirection direction) {
        if (region.dongCode() != null) return candidates(region, metricType, direction);
        return locationService.getDongs(region.sggCode()).parallelStream()
                .flatMap(dong -> candidates(new Region(region.sggCode(), dong.dongCd(),
                        region.name() + " " + dong.dongNm()), metricType, direction).stream())
                .toList();
    }

    private List<Candidate> candidates(Region region, String metricType, SortDirection direction) {
        TopAndBottomResponse response = topAndBottom(region.sggCode(), region.dongCode(), metricType);
        List<TopAndBottomResponse.BldgDealSummaryDto> source = direction == SortDirection.ASC
                ? response.bottom() : response.top();
        if (source == null) return List.of();
        return source.stream().map(item -> new Candidate(region.name(), response.baseDate(), item.bldgNm(),
                metricType.equals("pyeong") ? item.avgPyeongAmt() : item.avgThingAmt(),
                item.dealCnt() == null ? 0 : item.dealCnt())).toList();
    }

    private TopAndBottomResponse topAndBottom(String sggCode, String dongCode, String metricType) {
        CacheKey key = new CacheKey(sggCode, dongCode, metricType);
        CachedResponse cached = cache.get(key);
        if (cached != null && cached.createdAt().plus(CACHE_TTL).isAfter(Instant.now())) return cached.response();
        TopAndBottomResponse response = fastApiService.getTopAndBottom(new TopAndBottomRequest(sggCode, dongCode, metricType));
        cache.put(key, new CachedResponse(response, Instant.now()));
        return response;
    }

    private PriceRankingResponse toResponse(String regionName, String metricType, List<Candidate> candidates,
                                            RankingSearchQuery query) {
        SortDirection direction = query.direction();
        Comparator<Candidate> comparator = Comparator.comparing(Candidate::metricValue,
                Comparator.nullsLast(Comparator.naturalOrder()));
        if (direction == SortDirection.DESC) comparator = comparator.reversed();
        List<Candidate> sorted = candidates.stream()
                .filter(item -> item.metricValue() != null && item.dealCount() >= query.minimumTradeCount())
                .filter(item -> matchesPriceRange(item.metricValue(), metricType, query))
                .sorted(comparator).limit(query.limit()).toList();
        List<PriceRankingResponse.Item> items = java.util.stream.IntStream.range(0, sorted.size())
                .mapToObj(index -> {
                    Candidate item = sorted.get(index);
                    return new PriceRankingResponse.Item(index + 1, item.regionName(), item.apartmentName(),
                            item.metricValue(), item.dealCount());
                }).toList();
        String baseDate = sorted.isEmpty() ? null : sorted.get(0).baseDate();
        RankingCriteria criteria = new RankingCriteria(
                metricType.equals("pyeong") ? "평균 평단가" : "평균 거래가",
                metricType.equals("pyeong") ? "만원/평" : "만원",
                baseDate == null ? "최근 집계 기간" : baseDate + " 기준",
                query.minimumTradeCount(),
                direction == SortDirection.ASC ? "낮은 순" : "높은 순");
        return new PriceRankingResponse(regionName, metricType, baseDate, criteria, items);
    }

    private boolean matchesPriceRange(Long value, String metricType, RankingSearchQuery query) {
        if (!"thing_amt".equals(metricType)) return true;
        BigDecimal amountWon = BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(10_000));
        return (query.minPrice() == null || amountWon.compareTo(query.minPrice()) >= 0)
                && (query.maxPrice() == null || amountWon.compareTo(query.maxPrice()) < 0);
    }

    private boolean pyeongMetric(String question) {
        return question.contains("평당") || question.contains("평단") || question.contains("pyeong");
    }

    private Region resolveRegionLegacy(String question) {
        Matcher fullRegion = RegionQuestionPatterns.FULL_REGION.matcher(question);
        if (fullRegion.find()) {
            SggMaster sgg = findSgg(fullRegion.group(1));
            DongRegionResponse dong = locationService.resolveDong(fullRegion.group(2)).stream()
                    .filter(candidate -> candidate.sggCode().equals(sgg.getSggCode())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("자치구와 자치동 조합을 찾지 못했습니다."));
            return new Region(sgg.getSggCode(), dong.dongCode(), sgg.getSggName() + " " + dong.dongName());
        }
        Matcher district = RegionQuestionPatterns.DISTRICT.matcher(question);
        if (district.find()) {
            SggMaster sgg = findSgg(district.group(1));
            return new Region(sgg.getSggCode(), null, sgg.getSggName());
        }
        return null;
    }

    private SggMaster findSgg(String name) {
        return sggRepository.findBySggName(name)
                .orElseThrow(() -> new IllegalArgumentException(name + "을 찾지 못했습니다."));
    }

    private Region resolveRegion(String question) {
        RankingRegionResolver.ResolvedRegion resolved = regionResolver.resolve(question);
        if (resolved.allSeoul()) return null;
        return new Region(resolved.sggCode(), resolved.dongCode(), resolved.name());
    }

    private record Region(String sggCode, String dongCode, String name) {}
    private record Candidate(String regionName, String baseDate, String apartmentName, Long metricValue, int dealCount) {}
    private record CacheKey(String sggCode, String dongCode, String metricType) {}
    private record CachedResponse(TopAndBottomResponse response, Instant createdAt) {}
}
