package com.seoul.market.seoulmarketprice.report.dto.response;
import java.util.List;

/** 신고 목록과 페이지 메타데이터를 함께 전달하는 응답이다. */
public record ReportPageResponse(
        List<ReportListResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
