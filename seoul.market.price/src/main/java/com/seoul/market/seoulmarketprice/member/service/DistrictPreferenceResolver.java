package com.seoul.market.seoulmarketprice.member.service;

import java.util.List;

/** 회원 정보에서 헤더에 표시할 서울 자치구를 우선순위대로 결정한다. */
public final class DistrictPreferenceResolver {
    private static final String DEFAULT_DISTRICT = "중구";
    private static final List<String> SEOUL_DISTRICTS = List.of(
            "강남구", "강동구", "강북구", "강서구", "관악구",
            "광진구", "구로구", "금천구", "노원구", "도봉구",
            "동대문구", "동작구", "마포구", "서대문구", "서초구",
            "성동구", "성북구", "송파구", "양천구", "영등포구",
            "용산구", "은평구", "종로구", "중구", "중랑구"
    );

    private DistrictPreferenceResolver() {
    }

    public static String resolve(String myGu, String address) {
        String preferred = normalizeDistrict(myGu);
        if (preferred != null) {
            return preferred;
        }
        if (address != null && !address.isBlank()) {
            return SEOUL_DISTRICTS.stream()
                    .filter(address::contains)
                    .findFirst()
                    .orElse(DEFAULT_DISTRICT);
        }
        return DEFAULT_DISTRICT;
    }

    private static String normalizeDistrict(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return SEOUL_DISTRICTS.contains(trimmed) ? trimmed : null;
    }
}
