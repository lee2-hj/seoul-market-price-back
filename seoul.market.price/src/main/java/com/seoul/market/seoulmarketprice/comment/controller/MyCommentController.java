package com.seoul.market.seoulmarketprice.comment.controller;

import com.seoul.market.seoulmarketprice.comment.dto.condition.MyCommentSearchCondition;
import com.seoul.market.seoulmarketprice.comment.dto.response.MyCommentPageResponse;
import com.seoul.market.seoulmarketprice.comment.service.CommentService;
import com.seoul.market.seoulmarketprice.security.principal.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 마이페이지에서 로그인 사용자의 댓글 목록을 조회하는 API를 제공한다. */
@Tag(name = "내 댓글", description = "마이페이지 내 댓글 조회 API")
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class MyCommentController {

    /** 댓글 조회 업무를 처리하는 서비스다. */
    private final CommentService commentService;

    /** 현재 로그인한 사용자가 작성한 삭제되지 않은 댓글을 최신순으로 조회한다. */
    @Operation(summary = "내 댓글 목록 조회")
    @GetMapping("/me")
    public ResponseEntity<MyCommentPageResponse> getMyComments(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @ParameterObject
            @ModelAttribute MyCommentSearchCondition condition
    ) {
        return ResponseEntity.ok(
                commentService.getMyComments(principal.memberId(), condition)
        );
    }
}
