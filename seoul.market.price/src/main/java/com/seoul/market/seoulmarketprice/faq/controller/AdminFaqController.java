package com.seoul.market.seoulmarketprice.faq.controller;

import com.seoul.market.seoulmarketprice.faq.dto.request.FaqCreateRequest;
import com.seoul.market.seoulmarketprice.faq.dto.request.FaqUpdateRequest;
import com.seoul.market.seoulmarketprice.faq.dto.response.AdminFaqResponse;
import com.seoul.market.seoulmarketprice.faq.service.FaqService;
import com.seoul.market.seoulmarketprice.security.principal.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** 관리자의 FAQ 조회·등록·수정·삭제 API를 제공한다. */
@Tag(name = "관리자 FAQ", description = "FAQ 관리 API")
@RestController
@RequestMapping("/api/admin/faqs")
@RequiredArgsConstructor
public class AdminFaqController {

    /** FAQ 관리 비즈니스 로직을 처리하는 서비스이다. */
    private final FaqService faqService;

    /** 비노출 항목을 포함한 활성 FAQ 목록을 조회한다. */
    @Operation(summary = "관리자 FAQ 목록 조회")
    @GetMapping
    public ResponseEntity<List<AdminFaqResponse>> getFaqs(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(faqService.getAdminFaqs(keyword));
    }

    /** 관리자가 선택한 활성 FAQ 상세를 조회한다. */
    @Operation(summary = "관리자 FAQ 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<AdminFaqResponse> getFaq(@PathVariable Long id) {
        return ResponseEntity.ok(faqService.getAdminFaq(id));
    }

    /** 인증된 관리자 명의로 FAQ를 등록한다. */
    @Operation(summary = "FAQ 등록")
    @PostMapping
    public ResponseEntity<AdminFaqResponse> createFaq(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody FaqCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(faqService.createFaq(principal.memberId(), request));
    }

    /** FAQ의 입력된 필드만 수정한다. */
    @Operation(summary = "FAQ 수정")
    @PatchMapping("/{id}")
    public ResponseEntity<AdminFaqResponse> updateFaq(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody FaqUpdateRequest request
    ) {
        return ResponseEntity.ok(
                faqService.updateFaq(id, principal.memberId(), request)
        );
    }

    /** FAQ를 실제 제거하지 않고 삭제 시각을 기록한다. */
    @Operation(summary = "FAQ 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFaq(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        faqService.deleteFaq(id, principal.memberId());
        return ResponseEntity.noContent().build();
    }
}
