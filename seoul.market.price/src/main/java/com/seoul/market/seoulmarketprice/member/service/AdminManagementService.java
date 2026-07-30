package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.member.dto.request.AdminCreateRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.AdminCreateResponse;

/**
 * 관리자 계정 관리 기능을 정의한다.
 */
public interface AdminManagementService {

    /**
     * 관리자 계정을 생성한다.
     *
     * @param request 관리자 생성 요청
     * @return 생성된 관리자 기본 정보
     */
    AdminCreateResponse createAdmin(AdminCreateRequest request);
}
