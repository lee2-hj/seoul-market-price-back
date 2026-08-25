package com.seoul.market.seoulmarketprice.ai.dto;

import java.util.List;

public record NearbyApartmentResponse(
        String status,
        String message,
        int radiusMeters,
        String dataset,
        List<ApartmentCandidate> apartments
) {
    public record ApartmentCandidate(
            String apartmentId,
            String apartmentName,
            String address,
            String sggCode,
            String dongCode,
            double latitude,
            double longitude,
            long distanceMeters,
            Long averageTradeAmount,
            Long averagePyeongAmount,
            Integer dealCount,
            String latestDealDate,
            String baseDate
    ) {}
}
