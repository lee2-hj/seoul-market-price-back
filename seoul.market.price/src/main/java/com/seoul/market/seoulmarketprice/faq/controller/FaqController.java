package com.seoul.market.seoulmarketprice.faq.controller;

import com.seoul.market.seoulmarketprice.faq.dto.response.FaqPublicResponse;
import com.seoul.market.seoulmarketprice.faq.service.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 사용자에게 공개된 자주 묻는 질문 조회 API를 제공한다. */
@Tag(name = "자주 묻는 질문", description = "공개 FAQ 조회 API")
@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
public class FaqController {

    /** FAQ 조회 비즈니스 로직을 처리하는 서비스이다. */
    private final FaqService faqService;

    /** 공개 FAQ를 카테고리와 노출 순서에 맞춰 조회한다. */
    @Operation(summary = "FAQ 목록 조회")
    @GetMapping
    public ResponseEntity<List<FaqPublicResponse>> getFaqs(
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(faqService.getPublicFaqs(category));
    }

    /** 공개 FAQ 상세를 조회하고 조회수를 증가시킨다. */
    @Operation(summary = "FAQ 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<FaqPublicResponse> getFaq(@PathVariable Long id) {
        return ResponseEntity.ok(faqService.getPublicFaq(id));
    }
}
