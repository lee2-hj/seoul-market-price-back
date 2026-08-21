package com.seoul.market.seoulmarketprice.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PriceFacts(
        @NotBlank String regionA,
        @NotBlank String regionB,
        @Min(0) long regionAAverage,
        @Min(0) long regionBAverage,
        @NotBlank String unit,
        @NotBlank String higherRegion,
        @NotBlank String lowerRegion,
        @Min(0) long difference
) {}
