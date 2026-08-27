package com.seoul.market.seoulmarketprice.ai.dto;

import java.util.List;

public record TradeTrendRequest(String caseId, String question, TradeTrendFacts facts,
                                List<String> requiredFacts, List<String> forbiddenClaims) {}
