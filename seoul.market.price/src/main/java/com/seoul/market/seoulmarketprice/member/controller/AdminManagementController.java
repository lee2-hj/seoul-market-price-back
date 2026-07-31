package com.seoul.market.seoulmarketprice.member.controller;

import com.seoul.market.seoulmarketprice.member.dto.request.admin.AdminCreateRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminCreateResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminListResponse;
import com.seoul.market.seoulmarketprice.member.service.AdminManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @Operation(summary = "관리자 목록 조회")
    @GetMapping
    public ResponseEntity<List<AdminListResponse>> getAdmins() {
        return ResponseEntity.ok(adminManagementService.getAdmins());
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
}
