package com.seoul.market.seoulmarketprice.ai.controller;

import com.seoul.market.seoulmarketprice.ai.dto.*;
import com.seoul.market.seoulmarketprice.ai.service.*;
import com.seoul.market.seoulmarketprice.security.principal.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
/** AI 자연어 검색과 보조 도구 API의 HTTP 진입점이다. */
public class AiController {
    private final AiExplanationService aiExplanationService;
    private final NaturalLanguageSearchService naturalLanguageSearchService;
    private final QuestionAnalysisService questionAnalysisService;
    private final PlaceResolver placeResolver;
    private final NearbyApartmentSearchService nearbyApartmentSearchService;

    @PostMapping("/explain-price-comparison")
    /** 조회된 가격 비교 사실을 AI 설명 형식으로 변환한다. */
    public ResponseEntity<PriceComparisonResponse> explain(@Valid @RequestBody PriceComparisonRequest request) {
        return ResponseEntity.ok(aiExplanationService.explain(request));
    }

    @PostMapping("/search-natural")
    /** 사용자의 자연어 질문을 해석하고 적절한 부동산 검색을 수행한다. */
    public ResponseEntity<NaturalSearchResponse> searchNatural(
            @Valid @RequestBody AiSearchRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Long memberId = principal == null ? null : principal.memberId();
        return ResponseEntity.ok(naturalLanguageSearchService.search(request.question(), memberId));
    }

    @PostMapping("/analyze-question")
    /** 질문에서 의도, 지역, 지표와 후속 도구 실행 계획을 추출한다. */
    public ResponseEntity<QuestionAnalysisResponse> analyzeQuestion(
            @Valid @RequestBody QuestionAnalysisRequest request) {
        return ResponseEntity.ok(questionAnalysisService.analyze(request.question()));
    }

    @PostMapping("/tools/resolve-place")
    /** 장소명을 외부 장소 검색 결과와 서울 지역 정보로 해석한다. */
    public ResponseEntity<PlaceResolutionResponse> resolvePlace(
            @Valid @RequestBody PlaceResolveRequest request) {
        return ResponseEntity.ok(placeResolver.resolve(request.name(), request.type()));
    }

    @PostMapping("/tools/search-nearby-apartments")
    /** 좌표와 반경을 기준으로 인근 아파트 후보를 조회한다. */
    public ResponseEntity<NearbyApartmentResponse> searchNearbyApartments(
            @Valid @RequestBody NearbyApartmentRequest request) {
        return ResponseEntity.ok(nearbyApartmentSearchService.search(request));
    }
}
