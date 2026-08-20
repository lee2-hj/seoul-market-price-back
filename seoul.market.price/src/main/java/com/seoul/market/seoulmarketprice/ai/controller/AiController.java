package com.seoul.market.seoulmarketprice.ai.controller;

import com.seoul.market.seoulmarketprice.ai.dto.PriceComparisonRequest;
import com.seoul.market.seoulmarketprice.ai.dto.PriceComparisonResponse;
import com.seoul.market.seoulmarketprice.ai.dto.AiSearchRequest;
import com.seoul.market.seoulmarketprice.ai.service.AiSearchService;
import com.seoul.market.seoulmarketprice.ai.service.AiExplanationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
    private final AiExplanationService aiExplanationService;
    private final AiSearchService aiSearchService;

    @PostMapping("/explain-price-comparison")
    public ResponseEntity<PriceComparisonResponse> explain(
            @Valid @RequestBody PriceComparisonRequest request
    ) {
        return ResponseEntity.ok(aiExplanationService.explain(request));
    }

    @PostMapping("/search")
    public ResponseEntity<PriceComparisonResponse> search(@Valid @RequestBody AiSearchRequest request) {
        return ResponseEntity.ok(aiSearchService.search(request.question()));
    }
}
