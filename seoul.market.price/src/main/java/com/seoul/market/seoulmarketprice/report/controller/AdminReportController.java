package com.seoul.market.seoulmarketprice.report.controller;

import com.seoul.market.seoulmarketprice.report.dto.request.ReportAdminUpdateRequest;
import com.seoul.market.seoulmarketprice.report.dto.response.*;
import com.seoul.market.seoulmarketprice.report.entity.ReportCategory;
import com.seoul.market.seoulmarketprice.report.service.ReportService;
import com.seoul.market.seoulmarketprice.security.principal.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 관리자의 신고 조회·처리·삭제와 첨부파일 관리 API를 제공한다. */
@RestController @RequestMapping("/api/admin/reports") @RequiredArgsConstructor
public class AdminReportController {
    /** 신고 조회와 관리자 처리 로직. */
    private final ReportService reportService;

    /** 관리자가 전체 활성 신고를 조건별 페이지로 조회한다. */
    @GetMapping
    public ReportPageResponse reports(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size,
            @RequestParam(required=false) ReportCategory category,
            @RequestParam(required=false) String keyword) {
        return reportService.getReports(page, size, category, keyword);
    }
    /** 관리자가 비밀 여부와 관계없이 신고 상세 정보를 조회한다. */
    @GetMapping("/{id}") public ReportDetailResponse report(@PathVariable Long id) {
        return reportService.getReport(id, null, true);
    }
    /** 신고 처리 상태 또는 관리자 답변을 변경한다. */
    @PatchMapping("/{id}") public ReportDetailResponse update(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody ReportAdminUpdateRequest request) {
        return reportService.updateByAdmin(id, principal.memberId(), request);
    }
    /** 활성 신고를 소프트 삭제한다. */
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { reportService.deleteByAdmin(id); }
}
