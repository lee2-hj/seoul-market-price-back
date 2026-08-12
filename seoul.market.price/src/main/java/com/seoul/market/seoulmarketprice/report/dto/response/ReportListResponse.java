package com.seoul.market.seoulmarketprice.report.dto.response;
import com.seoul.market.seoulmarketprice.report.entity.*;
import java.time.LocalDateTime;

/** 신고 목록 화면에 필요한 요약 정보를 전달한다. */
public record ReportListResponse(
        Long id,
        ReportCategory category,
        ReportStatus status,
        /** 비밀 신고이면 외부 사용자 응답에서 {@code null}일 수 있다. */
        String targetProperty,
        /** 비밀 신고이면 외부 사용자 응답에서 {@code null}일 수 있다. */
        String title,
        /** 개인정보 노출을 줄이기 위해 마스킹한 작성자 이름. */
        String authorName,
        boolean isSecret,
        LocalDateTime createdAt
) {
}
