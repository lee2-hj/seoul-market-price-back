package com.seoul.market.seoulmarketprice.phoneverification.service;

import com.seoul.market.seoulmarketprice.phoneverification.client.PortOneIdentityVerificationClient;
import com.seoul.market.seoulmarketprice.phoneverification.dto.portone.PortOneIdentityVerificationResponse;
import com.seoul.market.seoulmarketprice.phoneverification.dto.request.PhoneVerificationConfirmRequest;
import com.seoul.market.seoulmarketprice.phoneverification.dto.response.PhoneVerificationConfirmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 회원가입 시 사용하는 휴대폰 PASS 본인인증(포트원 V2, KG이니시스 채널)
 * 비즈니스 로직을 처리하는 서비스이다.
 */
@Service
public class PhoneVerificationService {

    private static final Logger log =
            LoggerFactory.getLogger(PhoneVerificationService.class);

    /**
     * 포트원 서버에 본인인증 조회 완료 상태를 나타내는 값이다.
     */
    private static final String VERIFIED_STATUS = "VERIFIED";

    private final PortOneIdentityVerificationClient portOneIdentityVerificationClient;

    public PhoneVerificationService(
            PortOneIdentityVerificationClient portOneIdentityVerificationClient
    ) {
        this.portOneIdentityVerificationClient = portOneIdentityVerificationClient;
    }

    /**
     * 포트원 서버에 본인인증 결과를 조회하고, 실제로 인증이
     * 완료되었는지 확인한다.
     *
     * <p>
     * 프론트엔드가 전달한 결과를 그대로 믿지 않고, identityVerificationId로
     * 포트원 서버에 직접 조회한 결과만 신뢰한다.
     * </p>
     *
     * @param request identityVerificationId를 담은 요청
     * @return 인증된 이름/전화번호 등 본인인증 결과
     * @throws IllegalArgumentException 인증이 완료되지 않았거나
     *         (READY, FAILED) 존재하지 않는 인증 건인 경우
     * @throws IllegalStateException 인증 완료 상태인데 필요한 정보가
     *         응답에 없거나, 포트원 서버 오류로 조회에 실패한 경우
     */
    public PhoneVerificationConfirmResponse confirm(
            PhoneVerificationConfirmRequest request
    ) {
        PortOneIdentityVerificationResponse response =
                portOneIdentityVerificationClient.getIdentityVerification(
                        request.identityVerificationId()
                );

        if (!VERIFIED_STATUS.equals(response.status())) {

            String reason =
                    response.failure() != null
                            ? response.failure().reason()
                            : null;

            log.info(
                    "휴대폰 본인인증 미완료: identityVerificationId={}, status={}, reason={}",
                    request.identityVerificationId(),
                    response.status(),
                    reason
            );

            throw new IllegalArgumentException(
                    reason != null
                            ? "휴대폰 본인인증에 실패했습니다: " + reason
                            : "휴대폰 본인인증이 완료되지 않았습니다."
            );
        }

        PortOneIdentityVerificationResponse.VerifiedCustomer verifiedCustomer =
                response.verifiedCustomer();

        if (
                verifiedCustomer == null
                        || verifiedCustomer.name() == null
                        || verifiedCustomer.phoneNumber() == null
                        || verifiedCustomer.ci() == null
                        || verifiedCustomer.ci().isBlank()
        ) {
            throw new IllegalStateException(
                    "본인인증은 완료되었지만 인증된 회원 정보를 확인할 수 없습니다."
            );
        }

        log.info(
                "휴대폰 본인인증 완료: identityVerificationId={}",
                request.identityVerificationId()
        );

        return new PhoneVerificationConfirmResponse(
                true,
                verifiedCustomer.name(),
                verifiedCustomer.phoneNumber(),
                verifiedCustomer.birthDate(),
                verifiedCustomer.gender(),
                response.verifiedAt(),
                verifiedCustomer.ci()
        );
    }
}
