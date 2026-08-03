package com.seoul.market.seoulmarketprice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 백엔드가 알아야 하는 프론트엔드 주소 설정.
 *
 * <p>
 * OAuth2 로그인 완료 후 리다이렉트, CORS 허용 출처 등
 * 여러 곳에서 공통으로 사용한다. localhost:3000처럼 코드에
 * 직접 하드코딩하지 않고, 이 설정을 통해서만 참조한다.
 * </p>
 *
 * <p>
 * 로컬 개발에서는 프로젝트 루트의 .env.local(LOGIN_PAGE, ROOT_PAGE)에서,
 * 운영 환경에서는 동일한 이름의 OS 환경변수에서 값을 가져온다.
 * 스킴(http/https)은 포함하지 않은 host[:port][/path] 형태로 설정한다.
 * </p>
 *
 * @param loginPage 가입되지 않은 회원이 로그인 시도 시 이동할 프론트엔드 로그인 페이지 주소
 * @param rootPage  로그인 성공 시 이동할 프론트엔드 루트 페이지 주소이자 CORS 허용 출처
 */
@ConfigurationProperties(prefix = "frontend")
public record FrontendProperties(
        String loginPage,
        String rootPage
) {
}
