package com.seoul.market.seoulmarketprice.phoneverification.client;

import com.seoul.market.seoulmarketprice.config.PortOneProperties;
import com.seoul.market.seoulmarketprice.phoneverification.dto.portone.PortOneErrorResponse;
import com.seoul.market.seoulmarketprice.phoneverification.dto.portone.PortOneIdentityVerificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 포트원(PortOne) V2 본인인증 API를 호출하는 클라이언트이다.
 *
 * <p>
 * 프론트엔드는 포트원 브라우저 SDK로 PASS 본인인증(NHN KCP 채널)을
 * 직접 수행하고 identityVerificationId를 발급받는다. 프론트엔드가
 * 알려준 결과만 그대로 믿으면 위조될 수 있으므로, 백엔드는 이
 * 클라이언트로 포트원 서버에 직접 조회해서 실제 인증 결과를
 * 확인한다.
 * </p>
 */
@Component
public class PortOneIdentityVerificationClient {

    private static final Logger log =
            LoggerFactory.getLogger(PortOneIdentityVerificationClient.class);

    /**
     * 포트원 API 서버 주소이다.
     */
    private static final String BASE_URL = "https://api.portone.io";

    /**
     * 포트원 API 요청에 사용하는 클라이언트이다.
     *
     * <p>
     * Authorization 헤더는 "PortOne {API 시크릿}" 형식을 사용한다.
     * </p>
     */
    private final RestClient restClient;

    public PortOneIdentityVerificationClient(
            PortOneProperties portOneProperties
    ) {
        this.restClient =
                RestClient.builder()
                        .baseUrl(BASE_URL)
                        .defaultHeader(
                                HttpHeaders.AUTHORIZATION,
                                "PortOne " + portOneProperties.apiSecret()
                        )
                        .build();
    }

    /**
     * 본인인증 단건을 조회한다.
     *
     * @param identityVerificationId 조회할 본인인증 아이디
     * @return 포트원이 응답한 본인인증 내역
     * @throws IllegalArgumentException 존재하지 않는 아이디 등
     *         클라이언트 요청 오류(4xx)인 경우
     * @throws IllegalStateException 포트원 서버 오류(5xx) 등
     *         일시적인 통신 실패인 경우
     */
    public PortOneIdentityVerificationResponse getIdentityVerification(
            String identityVerificationId
    ) {
        try {
            return restClient.get()
                    .uri(
                            "/identity-verifications/{identityVerificationId}",
                            identityVerificationId
                    )
                    .retrieve()
                    .body(PortOneIdentityVerificationResponse.class);

        } catch (RestClientResponseException e) {

            String message = resolveErrorMessage(e);

            log.warn(
                    "포트원 본인인증 조회 실패: identityVerificationId={}, "
                            + "status={}, message={}",
                    identityVerificationId,
                    e.getStatusCode(),
                    message
            );

            if (e.getStatusCode().is5xxServerError()) {
                throw new IllegalStateException(
                        "본인인증 서버와 통신 중 오류가 발생했습니다: " + message
                );
            }

            throw new IllegalArgumentException(
                    "본인인증 정보를 확인할 수 없습니다: " + message
            );
        }
    }

    /**
     * 포트원 오류 응답 본문을 사람이 읽을 수 있는 메시지로 변환한다.
     *
     * <p>
     * 오류 응답 형식이 예상과 다르면(파싱 실패) 원본 예외 메시지를
     * 그대로 사용한다.
     * </p>
     *
     * @param e RestClient가 던진 응답 오류 예외
     * @return 사람이 읽을 수 있는 오류 메시지
     */
    private String resolveErrorMessage(RestClientResponseException e) {
        try {
            PortOneErrorResponse errorResponse =
                    e.getResponseBodyAs(PortOneErrorResponse.class);

            if (
                    errorResponse != null
                            && errorResponse.message() != null
            ) {
                return errorResponse.message();
            }
        } catch (Exception parseException) {
            // 오류 응답 파싱에 실패하면 아래에서 원본 메시지를 사용한다.
        }

        return e.getMessage();
    }
}
