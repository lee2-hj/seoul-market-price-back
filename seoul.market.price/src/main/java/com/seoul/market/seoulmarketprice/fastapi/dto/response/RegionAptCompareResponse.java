package com.seoul.market.seoulmarketprice.fastapi.dto.response;

public record RegionAptCompareResponse(
        AptGroupDto aptGroup1,
        AptGroupDto aptGroup2
) {
    public record AptGroupDto(
            String apt_name,
            Long avg_deal_price,
            Long avg_pyeong_price,
            Integer avg_pyeong,
            Integer latest_trade_pyeong,
            Integer deal_count,
            Integer total_households,
            Integer build_year,
            String use_approval_date
    ){}
}
