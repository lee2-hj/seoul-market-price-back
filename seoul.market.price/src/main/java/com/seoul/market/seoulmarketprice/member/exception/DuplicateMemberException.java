package com.seoul.market.seoulmarketprice.member.exception;

/**
 * 이미 사용 중인 사용자 아이디로 회원 생성을 시도할 때 발생하는 예외.
 *
 * <p>
 * Service의 사전 중복 확인과 DB UNIQUE 제약 위반을
 * 동일한 회원 중복 오류로 처리하기 위해 사용한다.
 * GlobalExceptionHandler가 이 예외를 HTTP 409 Conflict와
 * MEMBER-001 오류 응답으로 변환한다.
 * </p>
 */
public class DuplicateMemberException extends RuntimeException {

    /**
     * 사용자에게 전달할 중복 아이디 메시지로 예외를 생성한다.
     */
    public DuplicateMemberException() {
        super("이미 사용 중인 아이디입니다.");
    }
}
