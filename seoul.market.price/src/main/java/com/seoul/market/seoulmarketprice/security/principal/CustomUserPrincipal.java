package com.seoul.market.seoulmarketprice.security.principal;

import java.security.Principal;

/**
 * Spring Security에서 현재 로그인한 사용자를 표현하는 객체이다.
 *
 * <p>
 * JWT Access Token에서 꺼낸 회원 고유번호와 로그인 아이디를 담는다.
 * </p>
 *
 * <p>
 * Member 엔티티를 SecurityContext에 직접 저장하지 않기 때문에
 * 불필요한 DB 정보 노출과 영속성 관련 문제를 줄일 수 있다.
 * </p>
 *
 * <p>
 * 값 전달만 담당하는 불변 객체이므로 record로 작성한다.
 * </p>
 *
 * @param memberId 회원 고유번호
 * @param userId   로그인 아이디
 */
public record CustomUserPrincipal(
        Long memberId,
        String userId
) implements Principal {

    /** Spring Security의 인증 이름으로 로그인 아이디를 반환한다. */
    @Override
    public String getName() {
        return userId;
    }
}
