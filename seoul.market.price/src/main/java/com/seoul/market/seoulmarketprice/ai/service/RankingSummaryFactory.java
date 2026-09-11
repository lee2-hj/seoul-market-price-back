package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.RankingCriteria;

/** Produces a factual, deterministic title for ranking search results. */
public final class RankingSummaryFactory {
    private RankingSummaryFactory() {
    }

    public static String apartment(String regionName, RankingCriteria criteria, int itemCount) {
        return String.format("%s %s%s 아파트 %d곳입니다.", safeRegion(regionName), safeMetric(criteria),
                directionParticle(criteria), itemCount);
    }

    public static String district(String regionName, RankingCriteria criteria, int itemCount) {
        return String.format("%s %s%s 자치구 %d곳입니다.", safeRegion(regionName), safeMetric(criteria),
                directionParticle(criteria), itemCount);
    }

    private static String safeRegion(String regionName) {
        return regionName == null || regionName.isBlank() ? "서울" : regionName;
    }

    private static String safeMetric(RankingCriteria criteria) {
        return criteria == null || criteria.metric() == null || criteria.metric().isBlank()
                ? "순위" : criteria.metric();
    }

    private static String directionParticle(RankingCriteria criteria) {
        String direction = criteria == null ? null : criteria.sortDirection();
        return direction == null || direction.isBlank() ? " 기준" : " " + direction;
    }
}
