package com.seoul.market.seoulmarketprice.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuestionAnalysisRequest(
        @NotBlank @Size(max = 500) String question
) {}
