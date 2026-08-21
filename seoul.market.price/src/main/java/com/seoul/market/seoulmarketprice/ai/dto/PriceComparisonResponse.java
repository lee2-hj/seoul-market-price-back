package com.seoul.market.seoulmarketprice.ai.dto;

import java.util.List;

public record PriceComparisonResponse(
        String summary,
        List<String> keyPoints,
        List<String> cautions
) {}
