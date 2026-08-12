package com.seoul.market.seoulmarketprice.report.dto.request;
import com.seoul.market.seoulmarketprice.report.entity.ReportStatus;
import jakarta.validation.constraints.Size;

/** 관리자가 신고 상태 또는 답변 내용을 변경할 때 사용하는 요청이다. */
public record ReportAdminUpdateRequest(
        /** 변경할 처리 상태. 변경하지 않을 경우 {@code null}. */
        ReportStatus status,
        /** 사용자에게 제공할 관리자 답변. 변경하지 않을 경우 {@code null}. */
        @Size(max = 10000) String replyContent
) {
}
