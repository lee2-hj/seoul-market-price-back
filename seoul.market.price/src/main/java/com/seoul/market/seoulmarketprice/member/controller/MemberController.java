package com.seoul.market.seoulmarketprice.member.controller;

import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberCreateRequest;
import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberIdCheckRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberCreateResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberIdCheckResponse;
import com.seoul.market.seoulmarketprice.member.service.MemberService;
import com.seoul.market.seoulmarketprice.security.principal.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @PostMapping
    public ResponseEntity<MemberCreateResponse> createMember(
            @Valid @RequestBody MemberCreateRequest request
    ) {
        MemberCreateResponse response = memberService.createMember(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * 사용자 아이디의 중복 여부를 확인한다.
     */
    @Operation(summary = "아이디 중복 체크")
    @GetMapping("/check-id")
    public ResponseEntity<MemberIdCheckResponse> checkMemberId(
            @RequestBody MemberIdCheckRequest request
    ){
        MemberIdCheckResponse result = memberService.checkMemberId(request);
        return ResponseEntity
                .status(HttpStatus.OK).body(result);
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

//    @Operation(summary = "회원 정보 수정")
//    @PatchMapping("/me")
//    public ResponseEntity
}
