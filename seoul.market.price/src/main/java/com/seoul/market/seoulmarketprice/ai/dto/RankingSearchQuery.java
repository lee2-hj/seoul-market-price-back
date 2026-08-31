package com.seoul.market.seoulmarketprice.ai.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 순위형 자연어 질문을 DB 조회 조건으로 바꾼 값 객체다.
 * 금액 단위와 POPULARITY의 산식은 이후 검색 정책에서 일관되게 정의한다.
 */
public record RankingSearchQuery(
        RankingTarget target,
        RankingMetric metric,
        SortDirection direction,
        String regionCode,
        LocalDate from,
        LocalDate to,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal minPyeong,
        BigDecimal maxPyeong,
        BigDecimal threshold,
        int limit,
        int minimumTradeCount
) {
    public RankingSearchQuery {
        Objects.requireNonNull(target, "target은 필수입니다.");
        Objects.requireNonNull(metric, "metric은 필수입니다.");
        Objects.requireNonNull(direction, "direction은 필수입니다.");
        Objects.requireNonNull(from, "from은 필수입니다.");
        Objects.requireNonNull(to, "to는 필수입니다.");

        if (from.isAfter(to)) throw new IllegalArgumentException("조회 시작일은 종료일보다 늦을 수 없습니다.");
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("조회 개수는 1~100개여야 합니다.");
        if (minimumTradeCount < 0) throw new IllegalArgumentException("최소 거래량은 음수일 수 없습니다.");
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("최소 가격은 최대 가격보다 클 수 없습니다.");
        }
        if (minPyeong != null && maxPyeong != null && minPyeong.compareTo(maxPyeong) > 0) {
            throw new IllegalArgumentException("최소 평형이 최대 평형보다 클 수 없습니다.");
        }
    }
}
