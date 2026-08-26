package com.seoul.market.seoulmarketprice.member.controller;

import com.seoul.market.seoulmarketprice.member.dto.request.admin.AdminMemberUpdateRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminMemberPasswordResetResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminMemberPageResponse;
import com.seoul.market.seoulmarketprice.member.service.AdminMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

@Tag(name = "관리자 회원 관리", description = "관리자용 일반 회원 목록 API")
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {
    private final AdminMemberService adminMemberService;

    @Operation(summary = "일반 회원 목록 조회")
    @GetMapping
    public ResponseEntity<AdminMemberPageResponse> getMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminMemberService.getMembers(page, size));
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<Void> updateMember(@PathVariable Long memberId, @Valid @RequestBody AdminMemberUpdateRequest request) {
        adminMemberService.updateMember(memberId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{memberId}/password-reset")
    public ResponseEntity<AdminMemberPasswordResetResponse> resetPassword(
            @PathVariable Long memberId, @RequestParam(required = false) String email) {
        return ResponseEntity.ok(adminMemberService.resetPassword(memberId, email));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long memberId) {
        adminMemberService.deleteMember(memberId);
        return ResponseEntity.noContent().build();
    }
}
