package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.member.dto.request.admin.AdminCreateRequest;
import com.seoul.market.seoulmarketprice.member.dto.request.admin.AdminUpdateRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminCreateResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminPageResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminUpdateResponse;

/**
 * 관리자 계정 관리 기능을 정의한다.
 */
public interface AdminManagementService {

    AdminPageResponse getAdmins(int page, int size);

    /**
     * 관리자 계정을 생성한다.
     *
     * @param request 관리자 생성 요청
     * @return 생성된 관리자 기본 정보
     */
    AdminCreateResponse createAdmin(AdminCreateRequest request);

    /** 로그인 아이디를 제외한 관리자 정보를 선택적으로 수정한다. */
    AdminUpdateResponse updateAdmin(Long id, AdminUpdateRequest request);

    /** 자기 자신과 마지막 활성 관리자를 제외한 관리자 계정을 소프트 삭제한다. */
    void deleteAdmin(Long id, Long currentAdminId);
}
