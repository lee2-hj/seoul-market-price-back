package com.seoul.market.seoulmarketprice.ai.repository;

public record ApartmentLocation(
        String apartmentId,
        String apartmentName,
        String address,
        String sggCode,
        String dongCode,
        String districtName,
        String dongName,
        Double latitude,
        Double longitude,
        Long totalTradeAmount,
        Integer dealCount,
        Long averagePyeongAmount,
        Double exclusiveAreaM2,
        String latestDealDate,
        String baseDate
) {
    /** Backwards-compatible constructor for callers that do not provide Parquet place names. */
    public ApartmentLocation(String apartmentId, String apartmentName, String address, String sggCode, String dongCode,
                             Double latitude, Double longitude, Long totalTradeAmount, Integer dealCount,
                             Long averagePyeongAmount, Double exclusiveAreaM2, String latestDealDate, String baseDate) {
        this(apartmentId, apartmentName, address, sggCode, dongCode, null, null, latitude, longitude,
                totalTradeAmount, dealCount, averagePyeongAmount, exclusiveAreaM2, latestDealDate, baseDate);
    }

    public ApartmentLocation(String apartmentId, String apartmentName, String address, String sggCode, String dongCode,
                             Double latitude, Double longitude, Long totalTradeAmount, Integer dealCount,
                             Long averagePyeongAmount, String latestDealDate, String baseDate) {
        this(apartmentId, apartmentName, address, sggCode, dongCode, null, null, latitude, longitude, totalTradeAmount,
                dealCount, averagePyeongAmount, null, latestDealDate, baseDate);
    }

    public ApartmentLocation(String apartmentId, String apartmentName, String address,
                             String sggCode, String dongCode, Double latitude, Double longitude) {
        this(apartmentId, apartmentName, address, sggCode, dongCode, null, null, latitude, longitude,
                null, null, null, null, null, null);
    }

    public Long averageTradeAmount() {
        return totalTradeAmount == null || dealCount == null || dealCount < 1
                ? null : Math.round((double) totalTradeAmount / dealCount);
    }

    public String mainNumber() {
        return idPart(2);
    }

    public String subNumber() {
        return idPart(3);
    }

    private String idPart(int index) {
        String[] parts = apartmentId == null ? new String[0] : apartmentId.split("-");
        return parts.length > index ? parts[index] : null;
    }
}
