package com.seoul.market.seoulmarketprice.faq.exception;

/** 조회 조건에 맞는 활성 FAQ가 없을 때 발생한다. */
public class FaqNotFoundException extends RuntimeException {

    /** FAQ를 찾을 수 없음을 나타내는 예외를 생성한다. */
    public FaqNotFoundException() {
        super("자주 묻는 질문을 찾을 수 없습니다.");
    }
}
