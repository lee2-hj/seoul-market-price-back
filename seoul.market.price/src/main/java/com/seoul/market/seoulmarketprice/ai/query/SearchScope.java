package com.seoul.market.seoulmarketprice.ai.query;

/**
 * Resolving a question into a data boundary is separate from fetching or sorting data.
 * Null fields mean that the boundary does not constrain that dimension.
 */
public record SearchScope(
        Type type,
        String districtName,
        String dongName,
        String referencePlace,
        String apartmentName
) {
    public enum Type { ALL_SEOUL, DISTRICT, DONG, PLACE_RADIUS, APARTMENT, UNRESOLVED }

    public static SearchScope allSeoul() {
        return new SearchScope(Type.ALL_SEOUL, null, null, null, null);
    }
}
