package com.seoul.market.seoulmarketprice.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PriceComparisonRequest(
        @NotBlank String caseId,
        @NotBlank String question,
        @NotNull @Valid PriceFacts facts,
        List<String> requiredFacts,
        List<String> forbiddenClaims
) {
    public PriceComparisonRequest {
        requiredFacts = requiredFacts == null ? List.of() : List.copyOf(requiredFacts);
        forbiddenClaims = forbiddenClaims == null ? List.of() : List.copyOf(forbiddenClaims);
    }
}
