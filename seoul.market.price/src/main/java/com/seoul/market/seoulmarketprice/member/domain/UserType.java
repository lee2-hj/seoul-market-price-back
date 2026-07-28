package com.seoul.market.seoulmarketprice.member.domain;

/**
 * 사용자의 로그인 유형을 나타내는 열거형이다.
 *
 * <p>팀 공통 테이블 정의서의 {@code tb_user.user_type} 컬럼과 연결된다.</p>
 *
 * <ul>
 *     <li>0: 일반 로그인 사용자</li>
 *     <li>1: 소셜 로그인 사용자</li>
 * </ul>
 *
 * <p>코드에서 0과 1을 직접 사용하지 않고
 * {@code LOCAL}, {@code SOCIAL}이라는 의미 있는 이름으로 다루기 위해 사용한다.</p>
 */
public enum UserType {

    /*** 아이디와 비밀번호를 사용하는 일반 로그인 사용자.*/
    LOCAL(0),

    /*** 카카오 등 외부 OAuth 서비스를 사용하는 소셜 로그인 사용자.*/
    SOCIAL(1);

    /*** DB의 {@code user_type} 컬럼에 저장할 숫자값.*/
    private final int code;

    /*** 각 로그인 유형에 해당하는 DB 코드를 전달받는다.** @param code DB에 저장할 숫자값*/
    UserType(int code) {
        this.code = code;
    }

    /*** 현재 로그인 유형의 DB 코드를 반환한다. ** @return LOCAL은 0, SOCIAL은 1*/
    public int getCode() {
        return code;
    }

    /**
     * DB에서 조회한 숫자값을 UserType으로 변환한다.
     * @param code DB에서 조회한 user_type 값
     * @return 변환된 UserType
     * @throws IllegalArgumentException 지원하지 않는 값이 들어온 경우
     */
    public static UserType fromCode(int code) {
        return switch (code) {
            case 0 -> LOCAL;
            case 1 -> SOCIAL;
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 사용자 유형입니다. code=" + code
            );
        };
    }
}