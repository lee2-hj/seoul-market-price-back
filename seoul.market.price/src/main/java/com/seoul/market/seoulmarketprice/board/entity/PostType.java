package com.seoul.market.seoulmarketprice.board.entity;

/** 일반 게시글과 관리자 공지사항을 구분한다. */
public enum PostType {
    /** 일반 사용자가 작성한 게시글이다. */
    GENERAL(0),

    /** 관리자가 작성한 공지사항이다. */
    NOTICE(1);

    /** DB에 저장되는 게시글 유형 코드이다. */
    private final int value;

    /** DB 저장 코드를 사용하는 게시글 유형을 생성한다. */
    PostType(int value) {
        this.value = value;
    }

    /** DB에 저장할 게시글 유형 코드를 반환한다. */
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
