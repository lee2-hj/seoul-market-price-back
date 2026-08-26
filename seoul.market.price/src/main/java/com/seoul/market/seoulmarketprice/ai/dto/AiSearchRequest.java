package com.seoul.market.seoulmarketprice.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiSearchRequest(
        @NotBlank @Size(max = 300) String question
) {}
