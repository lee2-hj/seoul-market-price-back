package com.seoul.market.seoulmarketprice.menus.controller;

import com.seoul.market.seoulmarketprice.menus.dto.request.ActiveMenuCreateRequest;
import com.seoul.market.seoulmarketprice.menus.dto.request.ActiveMenuDeleteRequest;
import com.seoul.market.seoulmarketprice.menus.dto.response.ActiveMenuResponse;
import com.seoul.market.seoulmarketprice.menus.service.ActiveMenuService;
import com.seoul.market.seoulmarketprice.security.principal.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자별 활성 메뉴(사이드 메뉴 접근 권한)를 관리하는 Controller.
 *
 * <p>
 * {@code /me}는 principal에서 직접 로그인한 관리자의 고유번호를 사용하므로
 * 구조적으로 IDOR이 불가능하다. {@code /{id}} 계열은 다른 관리자의 활성 메뉴를
 * 조회/등록/해제하는 MASTER 전용 관리 기능이며, SecurityConfig에서
 * {@code hasRole("MASTER")}로 제한한다.
 * </p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "활성 메뉴", description = "관리자에 따른 접근 메뉴 관리")
@RequestMapping("/api/activeMenu")
public class ActiveMenuController {

    private static final String ROLE_MASTER = "ROLE_MASTER";

    private final ActiveMenuService activeMenuService;

    @GetMapping("/me")
    @Operation(summary = "내 활성 메뉴 조회", description = "로그인한 관리자 본인의 활성 메뉴 조회 api")
    public ResponseEntity<List<ActiveMenuResponse>> myActiveMenu(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(activeMenuService.getActiveMenu(principal.memberId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "활성 메뉴 조회", description = "MASTER 전용, 특정 관리자의 활성 메뉴 조회 api")
    public ResponseEntity<List<ActiveMenuResponse>> activeMenu(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            Authentication authentication
    ) {
        List<ActiveMenuResponse> response = activeMenuService.getActiveMenu(
                id,
                principal.memberId(),
                isMaster(authentication)
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{id}")
    @Operation(summary = "활성메뉴 등록", description = "MASTER 전용, 특정 관리자에게 활성 메뉴 등록 api")
    public ResponseEntity<Void> createActiveMenu(
            @PathVariable Long id,
            @RequestBody ActiveMenuCreateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            Authentication authentication
    ) {
        activeMenuService.createActiveMenu(id, request, principal.memberId(), isMaster(authentication));

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "활성메뉴 해제", description = "MASTER 전용, 특정 관리자의 활성 메뉴 해제 api")
    public ResponseEntity<Void> deleteActiveMenu(
            @PathVariable Long id,
            @RequestBody ActiveMenuDeleteRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            Authentication authentication
    ) {
        activeMenuService.deleteActiveMenu(id, request, principal.memberId(), isMaster(authentication));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 인증 토큰에 ROLE_MASTER 권한이 포함되어 있는지 확인한다. */
    private boolean isMaster(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_MASTER.equals(authority.getAuthority()));
    }
}
