package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.PlaceResolutionResponse;

public interface PlaceResolver {
    PlaceResolutionResponse resolve(String name, String type);
}
