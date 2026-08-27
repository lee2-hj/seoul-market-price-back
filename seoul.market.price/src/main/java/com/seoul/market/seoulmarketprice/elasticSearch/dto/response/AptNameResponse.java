package com.seoul.market.seoulmarketprice.elasticSearch.dto.response;

public record AptNameResponse(
        String apt_name,
        String mno,
        String sno,
        String dong_cd,
        String dong_nm,
        String sgg_cd,
        String sgg_nm
) {
}
