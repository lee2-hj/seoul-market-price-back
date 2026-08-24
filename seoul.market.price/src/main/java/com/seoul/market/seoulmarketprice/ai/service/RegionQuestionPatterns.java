package com.seoul.market.seoulmarketprice.ai.service;

import java.util.regex.Pattern;

/** 자연어 가격 검색에서 자치구와 행정동을 일관된 기준으로 구분한다. */
final class RegionQuestionPatterns {
    /** '성동구'의 '성동'처럼 자치구 이름 일부를 행정동으로 인식하지 않는다. */
    static final Pattern FULL_REGION = Pattern.compile("([가-힣]+구)\\s+([가-힣]+(?<!자치)동)(?!구)");
    static final Pattern DISTRICT = Pattern.compile("([가-힣]+구)");
    /** '자치동'은 검색 대상의 종류이며 실제 행정동 이름으로 추출하지 않는다. */
    static final Pattern DONG = Pattern.compile("([가-힣]+(?<!자치)동)(?!구)");

    private RegionQuestionPatterns() {
    }
}
