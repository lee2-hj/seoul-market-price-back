package com.seoul.market.seoulmarketprice.ai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record NearbyApartmentRequest(
        @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
        @Min(1) @Max(10000) Integer radiusMeters,
        @Min(1) @Max(50) Integer limit
) {}
