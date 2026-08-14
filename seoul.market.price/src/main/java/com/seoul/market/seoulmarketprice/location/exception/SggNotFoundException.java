package com.seoul.market.seoulmarketprice.location.exception;

/** 요청한 자치구 코드가 자치구 마스터에 없을 때 발생한다. */
public class SggNotFoundException extends RuntimeException {

    /** 조회하지 못한 자치구 코드를 포함한 예외를 생성한다. */
    public SggNotFoundException(String sggCode) {
        super("자치구를 찾을 수 없습니다: " + sggCode);
    }
}
