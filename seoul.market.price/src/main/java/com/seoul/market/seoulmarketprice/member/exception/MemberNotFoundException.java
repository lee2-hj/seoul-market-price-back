package com.seoul.market.seoulmarketprice.member.exception;

/** 요청 대상 회원이 존재하지 않거나 이미 탈퇴했을 때 발생한다. */
public class MemberNotFoundException extends RuntimeException {
    /** 외부에 회원 상태를 과도하게 노출하지 않는 공통 메시지를 설정한다. */
    public MemberNotFoundException() {
        super("활성 회원을 찾을 수 없습니다.");
    }
}
