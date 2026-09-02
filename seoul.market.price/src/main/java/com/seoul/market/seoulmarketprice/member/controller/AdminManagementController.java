package com.seoul.market.seoulmarketprice.member.controller;

import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import com.seoul.market.seoulmarketprice.auth.repository.AdminRepository;
import com.seoul.market.seoulmarketprice.member.dto.request.admin.AdminCreateRequest;
import com.seoul.market.seoulmarketprice.member.dto.request.admin.AdminUpdateRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminCreateResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminMeResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminPageResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminUpdateResponse;
import com.seoul.market.seoulmarketprice.member.exception.AdminNotFoundException;
import com.seoul.market.seoulmarketprice.member.service.AdminManagementService;
import com.seoul.market.seoulmarketprice.security.principal.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * 관리자 계정 생성 요청을 처리하는 Controller.
 *
 * <p>
 * 개발 환경에서는 최초 관리자 생성을 위해 공개할 수 있고,
 * 운영 환경에서는 ADMIN 권한을 가진 요청만 접근할 수 있다.
 * </p>
 */
@Tag(name = "관리자 관리", description = "관리자 계정 관리 API")
@RestController
@RequestMapping("/api/admins")
@RequiredArgsConstructor
public class AdminManagementController {

    private final AdminManagementService adminManagementService;
    private final AdminRepository adminRepository;

    /** 현재 로그인한 관리자 자신의 기본 정보를 조회한다. */
    @Operation(summary = "내 관리자 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<AdminMeResponse> me(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Admin admin = adminRepository.findById(principal.memberId())
                .orElseThrow(AdminNotFoundException::new);

        return ResponseEntity.ok(new AdminMeResponse(
                admin.getId(),
                admin.getAdminId(),
                admin.getName(),
                admin.getRole()
        ));
    }

    @Operation(summary = "관리자 목록 조회")
    @GetMapping
    public ResponseEntity<AdminPageResponse> getAdmins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminManagementService.getAdmins(page, size));
    }

    /**
     * 새 관리자 계정을 생성한다.
     *
     * @param request 관리자 생성 요청
     * @return 생성된 관리자 기본 정보
     */
    @Operation(summary = "관리자 계정 생성")
    @PostMapping
    public ResponseEntity<AdminCreateResponse> createAdmin(
            @Valid @RequestBody AdminCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(adminManagementService.createAdmin(request));
    }

    /** 로그인 아이디를 제외한 관리자 정보를 수정한다. */
    @Operation(summary = "관리자 정보 수정")
    @PatchMapping("/{id}")
    public ResponseEntity<AdminUpdateResponse> updateAdmin(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateRequest request
    ) {
        return ResponseEntity.ok(adminManagementService.updateAdmin(id, request));
    }

    /** 관리자 계정을 소프트 삭제하고 응답 본문 없이 성공을 반환한다. */
    @Operation(summary = "관리자 계정 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdmin(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        adminManagementService.deleteAdmin(id, principal.memberId());
        return ResponseEntity.noContent().build();
    }
}
