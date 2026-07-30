package com.seoul.market.seoulmarketprice.member.exception;

/**
 * 이미 사용 중인 아이디로 관리자 생성을 시도할 때 발생하는 예외.
 */
public class DuplicateAdminException extends RuntimeException {

    public DuplicateAdminException() {
        super("이미 사용 중인 관리자 아이디입니다.");
    }
}
