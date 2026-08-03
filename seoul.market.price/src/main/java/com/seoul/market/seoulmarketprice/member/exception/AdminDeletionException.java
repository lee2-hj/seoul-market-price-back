package com.seoul.market.seoulmarketprice.member.exception;

/** 관리자 계정을 안전하게 삭제할 수 없을 때 발생하는 예외이다. */
public class AdminDeletionException extends RuntimeException {
    public AdminDeletionException(String message) {
        super(message);
    }
}
