package com.seoul.market.seoulmarketprice.ai.dto;

import java.util.List;

public record TradeTrendResponse(String summary, List<String> keyPoints, List<String> cautions) {}
