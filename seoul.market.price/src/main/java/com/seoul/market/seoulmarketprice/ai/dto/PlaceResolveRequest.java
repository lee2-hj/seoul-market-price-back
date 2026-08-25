package com.seoul.market.seoulmarketprice.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record PlaceResolveRequest(@NotBlank String name, String type) {}
