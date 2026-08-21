package com.seoul.market.seoulmarketprice.ai.dto;

import java.util.List;

public record SingleRegionPriceRequest(String caseId, String question, SingleRegionFacts facts,
                                       List<String> requiredFacts, List<String> forbiddenClaims) {}
