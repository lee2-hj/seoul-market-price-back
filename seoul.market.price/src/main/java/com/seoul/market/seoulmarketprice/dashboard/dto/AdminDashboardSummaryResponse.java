package com.seoul.market.seoulmarketprice.dashboard.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 백오피스 대시보드의 당일 요약 지표 응답이다. */
public record AdminDashboardSummaryResponse(
        long totalUserCount,
        long todayUserCount,
        long totalBoardPostCount,
        long todayBoardPostCount,
        long totalQnaPostCount,
        long todayQnaPostCount,
        long todayTotalPostCount,
        LocalDate baseDate,
        OffsetDateTime generatedAt
) {
}
