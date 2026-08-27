package com.seoul.market.seoulmarketprice.comment.controller;

import com.seoul.market.seoulmarketprice.comment.dto.request.CommentCreateRequest;
import com.seoul.market.seoulmarketprice.comment.dto.request.CommentUpdateRequest;
import com.seoul.market.seoulmarketprice.comment.dto.response.CommentResponse;
import com.seoul.market.seoulmarketprice.comment.service.CommentService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 일반 사용자의 댓글 조회와 작성·수정·삭제 API를 제공한다. */
@Tag(name = "게시판 댓글", description = "일반 게시판 댓글과 대댓글 API")
@RestController
@RequestMapping("/api/boards/{boardId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** 게시글의 최상위 댓글과 대댓글을 계층 구조로 조회한다. */
    @Operation(summary = "댓글 목록 조회")
    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable Long boardId
    ) {
        return ResponseEntity.ok(commentService.getComments(boardId));
    }

    /** 로그인한 일반 사용자 명의로 최상위 댓글을 등록한다. */
    @Operation(summary = "댓글 작성")
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        CommentResponse response = commentService.createUserComment(
                boardId,
                principal.memberId(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /** 최상위 댓글 아래에 한 단계 대댓글을 등록한다. */
    @Operation(summary = "대댓글 작성")
    @PostMapping("/{parentId}/replies")
    public ResponseEntity<CommentResponse> createReply(
            @PathVariable Long boardId,
            @PathVariable Long parentId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        CommentResponse response = commentService.createUserReply(
                boardId,
                parentId,
                principal.memberId(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /** 현재 사용자가 작성한 활성 댓글의 내용을 수정한다. */
    @Operation(summary = "본인 댓글 수정")
    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long boardId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        return ResponseEntity.ok(
                commentService.updateUserComment(
                        boardId,
                        commentId,
                        principal.memberId(),
                        request
                )
        );
    }

    /** 현재 사용자가 작성한 댓글을 소프트 삭제한다. */
    @Operation(summary = "본인 댓글 삭제")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long boardId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        commentService.deleteUserComment(
                boardId,
                commentId,
                principal.memberId()
        );
        return ResponseEntity.noContent().build();
    }
}
