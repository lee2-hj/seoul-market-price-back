package com.seoul.market.seoulmarketprice.board.controller;

import com.seoul.market.seoulmarketprice.board.dto.request.AdminBoardUpdateRequest;
import com.seoul.market.seoulmarketprice.board.dto.request.NoticeCreateRequest;
import com.seoul.market.seoulmarketprice.board.dto.response.BoardDetailResponse;
import com.seoul.market.seoulmarketprice.board.service.BoardService;
import com.seoul.market.seoulmarketprice.security.principal.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자가 공지사항과 일반 게시글을 관리하는 API를 제공한다. */
@Tag(name = "관리자 게시판", description = "공지사항 및 게시글 관리 API")
@RestController
@RequestMapping("/api/admin/boards")
@RequiredArgsConstructor
public class AdminBoardController {

    /** 관리자 게시판 비즈니스 로직을 처리하는 서비스이다. */
    private final BoardService boardService;

    /** 관리자 명의의 공지사항을 새로 등록한다. */
    @Operation(summary = "공지사항 작성")
    @PostMapping("/notices")
    public ResponseEntity<BoardDetailResponse> createNotice(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody NoticeCreateRequest request
    ) {
        BoardDetailResponse response = boardService.createNotice(
                principal.memberId(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /** 게시글의 제목, 내용, 노출 여부 또는 고정 여부를 변경한다. */
    @Operation(summary = "게시글 관리자 수정")
    @PatchMapping("/{id}")
    public ResponseEntity<BoardDetailResponse> updateBoard(
            @PathVariable Long id,
            @Valid @RequestBody AdminBoardUpdateRequest request
    ) {
        return ResponseEntity.ok(
                boardService.updateByAdmin(id, request)
        );
    }

    /** 게시글을 실제로 제거하지 않고 삭제 일시를 기록한다. */
    @Operation(summary = "게시글 관리자 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long id
    ) {
        boardService.deleteByAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
