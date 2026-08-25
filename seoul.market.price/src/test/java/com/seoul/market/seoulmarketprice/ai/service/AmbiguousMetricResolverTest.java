package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import com.seoul.market.seoulmarketprice.ai.dto.RankingMetric;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AmbiguousMetricResolverTest {
    private final AmbiguousMetricResolver resolver = new AmbiguousMetricResolver();

    @Test
    void usesAvailableTradeCountProxyForPopularity() {
        var analysis = analysis("POPULARITY", List.of(
                new QuestionAnalysisResponse.MetricCandidate("FAVORITE_COUNT", 0.95, "직접 선호 지표"),
                new QuestionAnalysisResponse.MetricCandidate("TRADE_COUNT", 0.82, "거래 활동 대체 지표")
        ));

        var result = resolver.resolve(analysis);

        assertThat(result.metric()).isEqualTo(RankingMetric.TRADE_COUNT);
        assertThat(result.label()).isEqualTo("거래 건수");
    }

    @Test
    void doesNotEquateDislikeWithLowTradeCount() {
        var analysis = analysis("DISLIKE", List.of(
                new QuestionAnalysisResponse.MetricCandidate("NEGATIVE_RATING_COUNT", 0.94, "직접 부정 평가 지표")
        ));

        assertThatThrownBy(() -> resolver.resolve(analysis))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("부정 평가 수 데이터가 없습니다")
                .hasMessageContaining("거래량이 적은 아파트와 동일한 의미로 해석할 수 없습니다");
    }

    private QuestionAnalysisResponse analysis(String concept,
                                                List<QuestionAnalysisResponse.MetricCandidate> candidates) {
        return new QuestionAnalysisResponse("APARTMENT_RANKING", List.of(), null, "APARTMENT",
                "UNKNOWN", "DESC", 1, null, List.of("TRADE_COUNT"),
                List.of("GET_APARTMENT_RANKING"), List.of(), concept, candidates, false);
    }
}
