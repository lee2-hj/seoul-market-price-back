package com.seoul.market.seoulmarketprice.member.controller;

import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberCheckRequest;
import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberCreateRequest;
import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberIdCheckRequest;
import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberIdFindRequest;
import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberWithdrawalRequest;
import com.seoul.market.seoulmarketprice.member.dto.request.member.PasswordResetCompleteRequest;
import com.seoul.market.seoulmarketprice.member.dto.request.member.PasswordResetVerifyRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberCheckResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberCreateResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberIdCheckResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberIdFindResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberWithdrawalResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.member.PasswordResetCompleteResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.member.PasswordResetVerifyResponse;
import com.seoul.market.seoulmarketprice.member.service.MemberService;
import com.seoul.market.seoulmarketprice.member.service.MemberIdFindService;
import com.seoul.market.seoulmarketprice.member.service.PasswordResetService;
import com.seoul.market.seoulmarketprice.security.principal.CustomUserPrincipal;
import com.seoul.market.seoulmarketprice.security.jwt.RefreshTokenCookieManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 회원 가입과 회원 정보 조회 요청을 처리하는 Controller.
 *
 * <p>
 * 일반 회원가입은 별도의 인증 없이 처리되며,
 * 현재 로그인한 회원 조회는 Access Token 인증이 필요하다.
 * </p>
 */
@Tag(name = "회원 관리", description = "회원가입 및 회원정보 API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    /**
     * 회원 관련 비즈니스 로직을 처리하는 서비스.
     */
    private final MemberService memberService;

    private final MemberIdFindService memberIdFindService;

    private final PasswordResetService passwordResetService;

    /** 탈퇴 응답에서 Refresh Token 쿠키를 즉시 만료시킨다. */
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    /**
     * 아이디와 비밀번호를 사용하는 일반 회원을 생성한다.
     *
     * <p>
     * 요청 DTO 검증에 성공하면 회원 정보를 저장하고
     * HTTP 201 Created 상태로 생성된 회원의 기본 정보를 반환한다.
     * </p>
     *
     * @param request 회원가입 정보
     * @return 생성된 회원의 고유번호, 아이디, 이름
     */
    @Operation(summary = "일반 회원가입")
    @PostMapping("/signup")
    public ResponseEntity<MemberCreateResponse> createMember(
            @Valid @RequestBody MemberCreateRequest request
    ) {
        MemberCreateResponse response = memberService.createMember(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "회원 등록여부 조회")
    @GetMapping("/check-member")
    public ResponseEntity<MemberCheckResponse> checkMember(
            @ModelAttribute MemberCheckRequest request
    ){
        MemberCheckResponse response = memberService.checkMember(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 사용자 아이디의 중복 여부를 확인한다.
     */
    @Operation(summary = "아이디 중복 체크")
    @GetMapping("/check-id")
    public ResponseEntity<MemberIdCheckResponse> checkMemberId(
            @ModelAttribute MemberIdCheckRequest request
    ){
        MemberIdCheckResponse result = memberService.checkMemberId(request);
        return ResponseEntity
                .status(HttpStatus.OK).body(result);
    }

    /** PASS 본인인증 결과를 서버에서 확인하고 일치하는 아이디를 마스킹해 반환한다. */
    @Operation(
            summary = "아이디 찾기",
            description = "PASS 본인인증 식별자로 인증 결과를 확인한 뒤 "
                    + "일치하는 일반 회원 아이디를 마스킹하여 반환한다."
    )
    @PostMapping("/find-id")
    public ResponseEntity<MemberIdFindResponse> findMemberId(
            @Valid @RequestBody MemberIdFindRequest request
    ) {
        return ResponseEntity.ok(memberIdFindService.find(request));
    }

    /** PASS 인증자와 아이디의 회원 정보가 일치하면 단기 재설정 토큰을 발급한다. */
    @Operation(summary = "비밀번호 재설정 본인 확인")
    @PostMapping("/password-reset/verify")
    public ResponseEntity<PasswordResetVerifyResponse> verifyPasswordReset(
            @Valid @RequestBody PasswordResetVerifyRequest request
    ) {
        return ResponseEntity.ok(passwordResetService.verify(request));
    }

    /** 단기 재설정 토큰을 검증한 뒤 새 비밀번호를 적용한다. */
    @Operation(summary = "비밀번호 재설정 완료")
    @PostMapping("/password-reset/complete")
    public ResponseEntity<PasswordResetCompleteResponse> completePasswordReset(
            @Valid @RequestBody PasswordResetCompleteRequest request
    ) {
        return ResponseEntity.ok(passwordResetService.complete(request));
    }
    /**
     * Access Token으로 인증된 현재 회원의 기본 정보를 조회한다.
     *
     * <p>
     * JWT 인증 필터가 생성한 {@link CustomUserPrincipal}에서
     * 회원 고유번호와 사용자 아이디를 가져온다.
     * </p>
     *
     * @param principal 인증된 회원 정보
     * @return 현재 로그인한 회원의 기본 정보
     */
    @Operation(summary = "현재 로그인한 회원 조회")
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        // Entity를 직접 노출하지 않고 화면에 필요한 값만 응답한다.
        return ResponseEntity.ok(memberService.getMember(principal.memberId()));
    }

    /** 현재 비밀번호를 재확인한 뒤 로그인 회원을 소프트 삭제한다. */
    @Operation(summary = "현재 로그인한 일반 회원 탈퇴")
    @DeleteMapping("/me")
    public ResponseEntity<MemberWithdrawalResponse> withdraw(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody MemberWithdrawalRequest request
    ) {
        MemberWithdrawalResponse response = memberService.withdraw(
                principal.memberId(), request
        );
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookieManager.deleteRefreshTokenCookie().toString()
                )
                .body(response);
    }

//    @Operation(summary = "회원 정보 수정")
//    @PatchMapping("/me")
//    public ResponseEntity
}
