package com.seoul.market.seoulmarketprice.member.dto.response.member;

import com.seoul.market.seoulmarketprice.phoneverification.dto.response.MembershipStatus;

public record MemberCheckResponse(
        boolean isduplicated,
        MembershipStatus membershipStatus,
        boolean signupAllowed
) {
}
