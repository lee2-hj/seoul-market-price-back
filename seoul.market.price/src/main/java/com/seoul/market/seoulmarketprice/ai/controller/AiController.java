package com.seoul.market.seoulmarketprice.ai.controller;

import com.seoul.market.seoulmarketprice.ai.dto.*;
import com.seoul.market.seoulmarketprice.ai.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
    private final AiExplanationService aiExplanationService;
    private final NaturalLanguageSearchService naturalLanguageSearchService;
    private final QuestionAnalysisService questionAnalysisService;
    private final PlaceResolver placeResolver;
    private final NearbyApartmentSearchService nearbyApartmentSearchService;

    @PostMapping("/explain-price-comparison")
    public ResponseEntity<PriceComparisonResponse> explain(@Valid @RequestBody PriceComparisonRequest request) {
        return ResponseEntity.ok(aiExplanationService.explain(request));
    }

    @PostMapping("/search-natural")
    public ResponseEntity<NaturalSearchResponse> searchNatural(@Valid @RequestBody AiSearchRequest request) {
        return ResponseEntity.ok(naturalLanguageSearchService.search(request.question()));
    }

    @PostMapping("/analyze-question")
    public ResponseEntity<QuestionAnalysisResponse> analyzeQuestion(
            @Valid @RequestBody QuestionAnalysisRequest request) {
        return ResponseEntity.ok(questionAnalysisService.analyze(request.question()));
    }

    @PostMapping("/tools/resolve-place")
    public ResponseEntity<PlaceResolutionResponse> resolvePlace(
            @Valid @RequestBody PlaceResolveRequest request) {
        return ResponseEntity.ok(placeResolver.resolve(request.name(), request.type()));
    }

    @PostMapping("/tools/search-nearby-apartments")
    public ResponseEntity<NearbyApartmentResponse> searchNearbyApartments(
            @Valid @RequestBody NearbyApartmentRequest request) {
        return ResponseEntity.ok(nearbyApartmentSearchService.search(request));
    }
}
