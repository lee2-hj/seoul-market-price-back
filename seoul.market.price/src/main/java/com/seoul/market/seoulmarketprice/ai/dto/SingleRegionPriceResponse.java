package com.seoul.market.seoulmarketprice.ai.dto;

import java.util.List;

public record SingleRegionPriceResponse(String summary, List<String> keyPoints, List<String> cautions) {}
