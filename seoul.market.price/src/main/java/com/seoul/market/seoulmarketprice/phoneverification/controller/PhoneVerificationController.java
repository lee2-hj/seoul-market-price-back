package com.seoul.market.seoulmarketprice.phoneverification.controller;

import com.seoul.market.seoulmarketprice.phoneverification.dto.request.PhoneVerificationConfirmRequest;
import com.seoul.market.seoulmarketprice.phoneverification.dto.response.PhoneVerificationConfirmResponse;
import com.seoul.market.seoulmarketprice.phoneverification.service.PhoneVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원가입용 휴대폰 PASS 본인인증 요청을 처리하는 Controller.
 *
 * <p>
 * 로그인 전(회원가입 화면)에서 호출되므로 인증 없이 접근할 수 있다.
 * </p>
 */
@Tag(name = "본인인증", description = "회원가입 휴대폰 PASS 본인인증 API (포트원 V2, NHN KCP 채널)")
@RestController
@RequestMapping("/api/members/phone-verification")
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    public PhoneVerificationController(
            PhoneVerificationService phoneVerificationService
    ) {
        this.phoneVerificationService = phoneVerificationService;
    }

    /**
     * 포트원 브라우저 SDK로 완료한 PASS 본인인증 결과를 서버에서
     * 다시 확인한다.
     *
     * <p>
     * 프론트엔드가 전달한 인증 성공 여부를 그대로 믿지 않고,
     * identityVerificationId로 포트원 서버에 직접 조회한 결과만
     * 신뢰한다.
     * </p>
     *
     * @param request 포트원 브라우저 SDK가 발급한 identityVerificationId
     * @return 인증된 이름/전화번호 등 본인인증 결과
     */
    @Operation(
            summary = "휴대폰 PASS 본인인증 결과 확인",
            description = "프론트엔드에서 포트원 브라우저 SDK로 PASS 본인인증을 완료한 뒤 "
                    + "전달받은 identityVerificationId로, 포트원 서버에 실제 인증 결과를 "
                    + "조회하여 확인한다."
    )
    @PostMapping("/confirm")
    public ResponseEntity<PhoneVerificationConfirmResponse> confirm(
            @Valid @RequestBody PhoneVerificationConfirmRequest request
    ) {
        return ResponseEntity.ok(
                phoneVerificationService.confirm(request)
        );
    }
}
