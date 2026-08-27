package com.seoul.market.seoulmarketprice.ai.repository;

public record ApartmentLocation(
        String apartmentId,
        String apartmentName,
        String address,
        String sggCode,
        String dongCode,
        Double latitude,
        Double longitude,
        Long totalTradeAmount,
        Integer dealCount,
        Long averagePyeongAmount,
        String latestDealDate,
        String baseDate
) {
    public ApartmentLocation(String apartmentId, String apartmentName, String address,
                             String sggCode, String dongCode, Double latitude, Double longitude) {
        this(apartmentId, apartmentName, address, sggCode, dongCode, latitude, longitude,
                null, null, null, null, null);
    }

    public Long averageTradeAmount() {
        return totalTradeAmount == null || dealCount == null || dealCount < 1
                ? null : Math.round((double) totalTradeAmount / dealCount);
    }
}
