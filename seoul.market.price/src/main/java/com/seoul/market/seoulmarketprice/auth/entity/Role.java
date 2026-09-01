package com.seoul.market.seoulmarketprice.auth.entity;

/**
 * 로그인 사용자의 권한을 구분한다.
 *
 * <p>
 * 일반 사용자는 USER 권한을 사용하고,
 * 관리자는 ADMIN 권한을 사용한다.
 * </p>
 *
 * <p>
 * JWT Access Token에 권한 정보를 저장하고,
 * Spring Security에서 API 접근 권한을 검사할 때 사용한다.
 * </p>
 */
public enum Role {

    /**
     * 일반 사용자 권한.
     */
    USER,

    /**
     * 관리자 권한.
     */
    ADMIN,

    /** 모든 관리자 기능을 관리할 수 있는 최고 관리자 권한. */
    MASTER
}
