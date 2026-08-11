package com.seoul.market.seoulmarketprice.member.dto.response.member;

/** 회원 소프트 삭제가 완료되었음을 화면에 전달하는 응답이다. */
public record MemberWithdrawalResponse(
        /** 사용자에게 표시할 탈퇴 완료 안내 메시지. */
        String message
) {
}
