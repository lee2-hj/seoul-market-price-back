package com.seoul.market.seoulmarketprice.elasticSearch.dto.request;

public record AptNameRequest(

        // apt_name이 비어있으면 이름 조건 없이 sgg_cd/dong_cd만으로 목록을 조회한다
        // (ElasticSearchService.searchAptName 참고) - 그래서 더 이상 필수값이 아니다.
        String apt_name,
        String sgg_cd,
        String dong_cd
) {
}
