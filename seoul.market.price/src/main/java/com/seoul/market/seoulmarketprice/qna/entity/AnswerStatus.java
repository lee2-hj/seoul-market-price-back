package com.seoul.market.seoulmarketprice.qna.entity;

/** Q&A 게시글의 관리자 답변 처리 상태를 나타낸다. */
public enum AnswerStatus {
    /** 아직 관리자 답변이 등록되지 않은 상태이다. */
    WAITING(0),
    /** 관리자 답변이 등록된 상태이다. */
    COMPLETED(1);

    /** 데이터베이스에 저장되는 상태 코드이다. */
    private final int value;

    /** 상태 코드와 열거형 값을 연결한다. */
    AnswerStatus(int value) {
        this.value = value;
    }

    /** 데이터베이스 저장용 숫자 코드를 반환한다. */
    public int getValue() {
        return value;
    }

    /** 숫자 코드를 대응하는 답변 상태로 변환한다. */
    public static AnswerStatus fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (AnswerStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 답변 상태입니다: " + value);
    }
}
