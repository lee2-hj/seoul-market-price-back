package com.seoul.market.seoulmarketprice.board.controller;

import com.seoul.market.seoulmarketprice.board.dto.request.BoardCreateRequest;
import com.seoul.market.seoulmarketprice.board.dto.request.BoardUpdateRequest;
import com.seoul.market.seoulmarketprice.board.dto.response.BoardDetailResponse;
import com.seoul.market.seoulmarketprice.board.dto.response.BoardPageResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 일반 사용자가 게시글을 조회하고 작성·수정·삭제하는 API를 제공한다. */
@Tag(name = "일반 게시판", description = "일반 게시글 및 공지사항 조회 API")
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    /** 공개 게시글을 공지 우선, 최신순으로 조회한다. */
    @Operation(summary = "게시글 목록 조회")
    @GetMapping
    public ResponseEntity<BoardPageResponse> getBoards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(
                boardService.getBoards(page, size, keyword)
        );
    }

    /** 공개 게시글을 조회하고 조회수를 증가시킨다. */
    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<BoardDetailResponse> getBoard(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(boardService.getBoard(id));
    }

    /** 로그인한 일반 사용자 명의로 게시글을 등록한다. */
    @Operation(summary = "일반 게시글 작성")
    @PostMapping
    public ResponseEntity<BoardDetailResponse> createBoard(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody BoardCreateRequest request
    ) {
        BoardDetailResponse response = boardService.createBoard(
                principal.memberId(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /** 현재 사용자가 작성한 게시글의 제목과 내용을 수정한다. */
    @Operation(summary = "본인 게시글 수정")
    @PatchMapping("/{id}")
    public ResponseEntity<BoardDetailResponse> updateBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody BoardUpdateRequest request
    ) {
        return ResponseEntity.ok(
                boardService.updateBoard(id, principal.memberId(), request)
        );
    }

    /** 현재 사용자가 작성한 게시글을 소프트 삭제한다. */
    @Operation(summary = "본인 게시글 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        boardService.deleteBoard(id, principal.memberId());
        return ResponseEntity.noContent().build();
    }
}
