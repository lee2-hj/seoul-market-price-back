package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.RankingCriteria;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RankingSummaryFactoryTest {

    @Test
    void describesLowPriceRankingWithoutCallingItTopRanking() {
        RankingCriteria criteria = new RankingCriteria("평균 거래가", "만원", "최근 집계 기간", 0, "낮은 순");

        assertThat(RankingSummaryFactory.apartment("구로구", criteria, 5))
                .isEqualTo("구로구 평균 거래가 낮은 순 아파트 5곳입니다.");
    }

    @Test
    void describesHighPriceRankingWithItsActualDirection() {
        RankingCriteria criteria = new RankingCriteria("평균 거래가", "만원", "최근 집계 기간", 0, "높은 순");

        assertThat(RankingSummaryFactory.apartment("구로구", criteria, 1))
                .isEqualTo("구로구 평균 거래가 높은 순 아파트 1곳입니다.");
    }
}
