package com.seoul.market.seoulmarketprice.comment.controller;

import com.seoul.market.seoulmarketprice.comment.dto.request.CommentCreateRequest;
import com.seoul.market.seoulmarketprice.comment.dto.request.CommentVisibilityRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자가 댓글을 작성하고 노출 및 삭제 상태를 관리하는 API를 제공한다. */
@Tag(name = "관리자 댓글", description = "관리자 댓글 작성과 노출 관리 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentService commentService;

    /** 활성 게시글에 관리자 명의의 최상위 댓글을 등록한다. */
    @Operation(summary = "관리자 댓글 작성")
    @PostMapping("/boards/{boardId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        CommentResponse response = commentService.createAdminComment(
                boardId,
                principal.memberId(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /** 댓글의 공개 또는 숨김 상태를 변경한다. */
    @Operation(summary = "댓글 노출 여부 변경")
    @PatchMapping("/comments/{commentId}/visibility")
    public ResponseEntity<CommentResponse> changeVisibility(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentVisibilityRequest request
    ) {
        return ResponseEntity.ok(
                commentService.changeVisibility(commentId, request.visible())
        );
    }

    /** 댓글을 실제로 제거하지 않고 삭제 일시를 기록한다. */
    @Operation(summary = "댓글 관리자 삭제")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId
    ) {
        commentService.deleteByAdmin(commentId);
        return ResponseEntity.noContent().build();
    }
}
