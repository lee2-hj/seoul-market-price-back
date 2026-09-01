package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.DistrictRankingResponse;
import com.seoul.market.seoulmarketprice.ai.dto.RankingCriteria;
import com.seoul.market.seoulmarketprice.ai.query.DataSourceAdapter;
import com.seoul.market.seoulmarketprice.ai.query.GenericQueryExecutor;
import com.seoul.market.seoulmarketprice.ai.query.MetricRecord;
import com.seoul.market.seoulmarketprice.ai.query.QueryRequest;
import com.seoul.market.seoulmarketprice.ai.query.SearchScope;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * District ranking response assembler.
 * Retrieval and query rules live in the shared adapter/executor layer so future scopes
 * use the same filtering and sorting contract.
 */
@Service
public class DistrictRankingSearchService {
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;
    private static final int MINIMUM_TRADE_COUNT = 3;
    private static final Pattern LIMIT = Pattern.compile("(\\d+)\\s*(?:개|곳|위)");

    private final DataSourceAdapter districtMetricDataSourceAdapter;
    private final GenericQueryExecutor queryExecutor;

    public DistrictRankingSearchService(@Qualifier("districtMetricDataSourceAdapter") DataSourceAdapter districtMetricDataSourceAdapter,
                                        GenericQueryExecutor queryExecutor) {
        this.districtMetricDataSourceAdapter = districtMetricDataSourceAdapter;
        this.queryExecutor = queryExecutor;
    }

    public DistrictRankingResponse search(String question) {
        int limit = limit(question);
        boolean ascending = question.contains("낮은") || question.contains("최저")
                || question.contains("싼") || question.contains("저렴");
        SearchScope scope = SearchScope.allSeoul();
        if (!districtMetricDataSourceAdapter.supports(scope)) {
            throw new IllegalStateException("자치구 순위 데이터 소스를 사용할 수 없습니다.");
        }

        List<MetricRecord> ranked = queryExecutor.execute(
                districtMetricDataSourceAdapter.fetch(scope),
                new QueryRequest((long) MINIMUM_TRADE_COUNT, null, null, null, null,
                        QueryRequest.SortField.AVERAGE_PYEONG_PRICE, ascending, limit));
        if (ranked.isEmpty()) {
            throw new IllegalArgumentException("서울시 자치구 평단가 데이터가 없습니다.");
        }

        List<DistrictRankingResponse.Item> items = java.util.stream.IntStream.range(0, ranked.size())
                .mapToObj(index -> {
                    MetricRecord value = ranked.get(index);
                    return new DistrictRankingResponse.Item(index + 1, value.districtName(),
                            value.averagePyeongPriceManwon(), value.tradeCount());
                }).toList();
        String baseDate = ranked.stream().map(MetricRecord::baseDate)
                .filter(value -> value != null && !value.isBlank()).findFirst().orElse(null);
        RankingCriteria criteria = new RankingCriteria("평균 평당 가격", "만원/평",
                baseDate == null ? "최근 집계 기간" : baseDate + " 기준",
                MINIMUM_TRADE_COUNT, ascending ? "낮은 순" : "높은 순");
        return new DistrictRankingResponse("서울 전체", "district_pyeong", baseDate, criteria, items);
    }

    private int limit(String question) {
        Matcher matcher = LIMIT.matcher(question);
        int requested = matcher.find() ? Integer.parseInt(matcher.group(1)) : DEFAULT_LIMIT;
        if (requested < 1 || requested > MAX_LIMIT) {
            throw new IllegalArgumentException("자치구 순위는 1개부터 10개까지 조회할 수 있습니다.");
        }
        return requested;
    }
}
