package com.seoul.market.seoulmarketprice.member.dto.response.member;

import java.util.List;

/**
 * PASS 본인인증 정보와 일치하는 회원 아이디를 마스킹하여 반환한다.
 * 원본 아이디는 응답에 포함하지 않는다.
 */
public record MemberIdFindResponse(
        boolean found,
        List<String> maskedUserIds
) {
    public MemberIdFindResponse {
        maskedUserIds = List.copyOf(maskedUserIds);
    }
}
