package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.member.dto.request.MemberCreateRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.MemberCreateResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.UserIdCheckResponse;

/**
 * 회원 관리 기능이 제공해야 하는 비즈니스 로직을 정의한다.
 *
 * <p>
 * Controller는 구현 클래스에 직접 의존하지 않고
 * 이 인터페이스를 통해 아이디 확인과 회원가입을 요청한다.
 * </p>
 */
public interface MemberService {

    /**
     * 사용자 아이디의 사용 가능 여부를 확인한다.
     *
     * @param userId 확인할 사용자 아이디
     * @return 아이디와 사용 가능 여부
     */
    UserIdCheckResponse checkUserId(String userId);

    /**
     * 일반 회원을 생성한다.
     *
     * @param request 회원가입 요청 정보
     * @return 생성된 회원의 기본 정보
     */
    MemberCreateResponse createMember(MemberCreateRequest request);
}
