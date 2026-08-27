package com.seoul.market.seoulmarketprice.report.controller;

import com.seoul.market.seoulmarketprice.report.dto.request.ReportCreateRequest;
import com.seoul.market.seoulmarketprice.report.dto.response.*;
import com.seoul.market.seoulmarketprice.report.entity.ReportCategory;
import com.seoul.market.seoulmarketprice.report.service.ReportService;
import com.seoul.market.seoulmarketprice.security.principal.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 사용자 신고 조회·등록·삭제와 첨부파일 API를 제공한다. */
@RestController @RequestMapping("/api/reports") @RequiredArgsConstructor
public class ReportController {
    /** 신고 비즈니스 로직. */
    private final ReportService reportService;

    /** 공개 신고 목록을 검색 조건과 페이지 조건으로 조회한다. */
    @GetMapping
    public ReportPageResponse getReports(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size,
            @RequestParam(required=false) ReportCategory category,
            @RequestParam(required=false) String keyword) {
        return reportService.getReports(page, size, category, keyword);
    }

    @GetMapping("/{id}")
    public ReportDetailResponse getReport(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal, Authentication authentication) {
        return reportService.getReport(id, principal == null ? null : principal.memberId(), isAdmin(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportDetailResponse createReport(@AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody ReportCreateRequest request) {
        return reportService.createReport(principal.memberId(), request);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReport(@PathVariable Long id, @AuthenticationPrincipal CustomUserPrincipal principal) {
        reportService.deleteByOwner(id, principal.memberId());
    }

    /** 현재 인증 객체에 관리자 권한이 포함되어 있는지 확인한다. */
    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
    /** 공개 신고 또는 접근 권한이 있는 비밀 신고의 상세 정보를 조회한다. */
    /** 로그인 회원 명의로 신고를 새로 등록한다. */
    /** 작성자 본인의 신고를 소프트 삭제한다. */
    /** 작성자 본인의 신고에 첨부파일을 업로드한다. */
    /** 신고 접근 권한을 확인한 뒤 첨부파일 목록을 조회한다. */
    /** 신고 접근 권한을 확인한 뒤 첨부파일 다운로드 정보를 반환한다. */
    /** 작성자 본인의 신고 첨부파일을 삭제한다. */
