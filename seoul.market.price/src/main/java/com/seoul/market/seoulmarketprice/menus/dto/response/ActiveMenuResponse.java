package com.seoul.market.seoulmarketprice.menus.dto.response;

import java.time.LocalDateTime;

/**
 * 관리자별로 활성화된 메뉴 항목을 표현하는 응답 DTO이다.
 *
 * @param id           활성 메뉴 고유번호
 * @param adminId      대상 관리자 고유번호
 * @param categoryCode 메뉴 카테고리 코드
 * @param categoryName 메뉴 카테고리명
 * @param menuCode     메뉴 코드
 * @param menuName     메뉴명
 * @param url          메뉴 진입 시 이동할 프론트엔드 경로
 * @param createdAt    등록일
 * @param updatedAt    수정일
 */
public record ActiveMenuResponse(
        Long id,
        Long adminId,
        String categoryCode,
        String categoryName,
        String menuCode,
        String menuName,
        String url,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
