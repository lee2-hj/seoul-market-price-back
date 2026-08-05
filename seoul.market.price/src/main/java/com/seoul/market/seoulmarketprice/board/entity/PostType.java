package com.seoul.market.seoulmarketprice.board.entity;

/** 일반 게시글과 관리자 공지사항을 구분한다. */
public enum PostType {
    GENERAL(0),
    NOTICE(1);

    private final int value;

    PostType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /** DB 숫자 값을 게시글 유형으로 변환한다. */
    public static PostType fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (PostType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 게시글 구분입니다: " + value);
    }
}
