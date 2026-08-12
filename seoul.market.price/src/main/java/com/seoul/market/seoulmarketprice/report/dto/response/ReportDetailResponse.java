package com.seoul.market.seoulmarketprice.report.dto.response;
import com.seoul.market.seoulmarketprice.report.entity.*;
import java.time.LocalDateTime;

/** 신고 상세 화면에 필요한 신고 내용과 관리자 답변 정보를 전달한다. */
public record ReportDetailResponse(
        Long id,
        ReportCategory category,
        ReportStatus status,
        String targetProperty,
        String title,
        String content,
        /** 개인정보 노출을 줄이기 위해 마스킹한 작성자 이름. */
        String authorName,
        Long authorMemberId,
        boolean isSecret,
        String adminReply,
        String adminName,
        LocalDateTime repliedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
