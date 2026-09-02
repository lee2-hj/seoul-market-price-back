package com.seoul.market.seoulmarketprice.menus.controller;

import com.seoul.market.seoulmarketprice.auth.entity.Role;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자별 활성 메뉴(사이드 메뉴 접근 권한)를 관리하는 Controller.
 *
 * <p>
 * {@code /me}는 role에 따라 반환 범위가 다르다: MASTER는 등록 여부와 무관하게
 * 전체 메뉴 카탈로그를, 그 외(ADMIN)는 자신에게 실제 등록된 메뉴만 받는다. 이
 * role 분기는 컨트롤러 레벨에서 {@link CustomUserPrincipal#role()}로 판단하고,
 * 서비스는 두 메서드를 분기 없이 제공한다.
 * </p>
 *
 * <p>
 * {@code GET /{id}}(조회)는 role과 무관하게 로그인한 사용자면 누구나 호출할 수
 * 있다(원래 동작으로 유지). 반면 {@code POST/DELETE /{id}}(등록/해제)는 관리
 * 기능이므로 MASTER는 임의의 {@code id}를, 그 외는 자기 자신의 {@code id}만
 * 대상으로 할 수 있다({@link ActiveMenuService}의 서비스 레벨 IDOR 검증).
 * </p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "활성 메뉴", description = "관리자에 따른 접근 메뉴 관리")
@RequestMapping("/api/activeMenu")
public class ActiveMenuController {

    private final ActiveMenuService activeMenuService;

    @GetMapping("/me")
    @Operation(
            summary = "내 활성 메뉴 조회",
            description = "MASTER는 전체 메뉴 카탈로그를, 그 외는 본인에게 등록된 활성 메뉴만 조회한다."
    )
    public ResponseEntity<List<ActiveMenuResponse>> myActiveMenu(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        List<ActiveMenuResponse> response = principal.role() == Role.MASTER
                ? activeMenuService.getAllMenusForMaster(principal.memberId())
                : activeMenuService.getActiveMenu(principal.memberId());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "활성 메뉴 조회", description = "로그인한 사용자라면 role과 무관하게 특정 관리자의 활성 메뉴를 조회할 수 있다.")
    public ResponseEntity<List<ActiveMenuResponse>> activeMenu(
            @PathVariable Long id
    ) {
        List<ActiveMenuResponse> response = activeMenuService.getActiveMenu(id);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{id}")
    @Operation(summary = "활성메뉴 등록", description = "ADMIN·MASTER, 특정 관리자에게 활성 메뉴 등록 api(ADMIN은 본인만)")
    public ResponseEntity<Void> createActiveMenu(
            @PathVariable Long id,
            @RequestBody ActiveMenuCreateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        activeMenuService.createActiveMenu(id, request, principal.memberId(), principal.role() == Role.MASTER);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "활성메뉴 해제", description = "ADMIN·MASTER, 특정 관리자의 활성 메뉴 해제 api(ADMIN은 본인만)")
    public ResponseEntity<Void> deleteActiveMenu(
            @PathVariable Long id,
            @RequestBody ActiveMenuDeleteRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        activeMenuService.deleteActiveMenu(id, request, principal.memberId(), principal.role() == Role.MASTER);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
