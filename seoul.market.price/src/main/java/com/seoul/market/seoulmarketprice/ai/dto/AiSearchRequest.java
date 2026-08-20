package com.seoul.market.seoulmarketprice.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiSearchRequest(@NotBlank String question) {}
