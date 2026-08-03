package com.seoul.market.seoulmarketprice.member.exception;

/** 활성 상태인 관리자를 찾을 수 없을 때 발생하는 예외이다. */
public class AdminNotFoundException extends RuntimeException {
    public AdminNotFoundException() {
        super("관리자를 찾을 수 없습니다.");
    }
}
