package com.seoul.market.seoulmarketprice.faq.service;

import com.seoul.market.seoulmarketprice.faq.dto.request.FaqCreateRequest;
import com.seoul.market.seoulmarketprice.faq.dto.request.FaqUpdateRequest;
import com.seoul.market.seoulmarketprice.faq.dto.response.AdminFaqResponse;
import com.seoul.market.seoulmarketprice.faq.dto.response.FaqPublicResponse;

import java.util.List;

/** 사용자와 관리자가 사용하는 FAQ 기능의 서비스 계약이다. */
public interface FaqService {

    /** 공개 FAQ 목록을 선택한 카테고리에 맞춰 조회한다. */
    List<FaqPublicResponse> getPublicFaqs(String category);

    /** 공개 FAQ 상세를 조회하고 조회수를 증가시킨다. */
    FaqPublicResponse getPublicFaq(Long id);

    /** 관리 화면에서 삭제되지 않은 FAQ 목록을 조회한다. */
    List<AdminFaqResponse> getAdminFaqs();

    /** 관리 화면에서 FAQ 상세를 조회한다. */
    AdminFaqResponse getAdminFaq(Long id);

    /** 관리자 명의로 FAQ를 등록한다. */
    AdminFaqResponse createFaq(Long memberId, FaqCreateRequest request);

    /** FAQ를 수정하고 작업한 관리자 인덱스를 기록한다. */
    AdminFaqResponse updateFaq(Long id, Long memberId, FaqUpdateRequest request);

    /** FAQ를 소프트 삭제한다. */
    void deleteFaq(Long id, Long memberId);
}
