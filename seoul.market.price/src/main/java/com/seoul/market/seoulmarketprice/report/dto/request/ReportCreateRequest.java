package com.seoul.market.seoulmarketprice.report.dto.request;
import com.seoul.market.seoulmarketprice.report.entity.ReportCategory;
import jakarta.validation.constraints.*;

/** 로그인한 회원이 새로운 신고를 등록할 때 사용하는 요청이다. */
public record ReportCreateRequest(
        /** 신고 유형. */
        @NotNull ReportCategory category,
        /** 신고 대상 매물 또는 대상을 식별할 수 있는 설명. */
        @NotBlank @Size(max = 200) String targetProperty,
        /** 신고 제목. */
        @NotBlank @Size(max = 200) String title,
        /** 신고 상세 내용. */
        @NotBlank @Size(max = 10000) String content,
        /** 작성자와 관리자 외 사용자에게 상세 내용을 숨길지 여부. */
        boolean isSecret
) {
}
