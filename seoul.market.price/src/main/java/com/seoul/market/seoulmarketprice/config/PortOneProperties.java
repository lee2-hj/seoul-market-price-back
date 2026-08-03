package com.seoul.market.seoulmarketprice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 포트원(PortOne) V2 API 연동 설정값.
 *
 * <p>
 * 휴대폰 PASS 본인인증(NHN KCP 채널)에 사용한다.
 * api-secret은 로컬 개발에서는 프로젝트 루트의
 * .env.local(PORTONE_API_SECRET)에서, 운영 환경에서는 동일한
 * 이름의 OS 환경변수에서 값을 가져온다.
 * </p>
 *
 * @param apiSecret  포트원 콘솔 > 결제 연동 > API Keys에서 발급받는 V2 API 시크릿
 * @param storeId    포트원 콘솔에 등록된 상점 아이디
 * @param channelKey 포트원 콘솔에 등록된 채널키(KCP 채널)
 */
@ConfigurationProperties(prefix = "portone")
public record PortOneProperties(
        String apiSecret,
        String storeId,
        String channelKey
) {
}
