package com.seoul.market.seoulmarketprice.menus.exception;

/**
 * MASTER가 아닌 관리자가 자기 자신이 아닌 다른 관리자의 활성 메뉴에
 * 접근하려고 할 때 발생하는 예외이다.
 *
 * <p>
 * SecurityConfig의 {@code /api/activeMenu/**} 인가 규칙(MASTER 전용)이
 * 1차 방어선이며, 이 예외는 서비스 레이어의 2차 방어선이다.
 * </p>
 */
public class ActiveMenuAccessDeniedException extends RuntimeException {
    public ActiveMenuAccessDeniedException() {
        super("다른 관리자의 활성 메뉴에 접근할 권한이 없습니다.");
    }
}
