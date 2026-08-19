package com.seoul.market.seoulmarketprice.elasticSearch.controller;

import com.seoul.market.seoulmarketprice.elasticSearch.dto.request.AptNameRequest;
import com.seoul.market.seoulmarketprice.elasticSearch.dto.response.AptNameResponse;
import com.seoul.market.seoulmarketprice.elasticSearch.service.ElasticSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "엘라스틱서치", description = "엘라스틱서치 API 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/elasticSearch")
public class ElasticSearchController {

    private final ElasticSearchService elasticSearchService;

    @Operation(summary = "아파트명 자동완성 검색", description = "apt_name으로 자동완성 검색하고 mno, sno가 있으면 필터로 사용한다.")
    @GetMapping("/aptname")
    public ResponseEntity<List<AptNameResponse>> searchAptName(@Valid @ModelAttribute AptNameRequest request) {
        List<AptNameResponse> response = elasticSearchService.searchAptName(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
