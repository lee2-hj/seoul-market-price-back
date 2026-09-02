package com.seoul.market.seoulmarketprice.qna.controller;

import com.seoul.market.seoulmarketprice.board.attachment.dto.AttachmentDownloadResponse;
import com.seoul.market.seoulmarketprice.board.attachment.dto.AttachmentResponse;
import com.seoul.market.seoulmarketprice.board.attachment.entity.AttachmentTargetType;
import com.seoul.market.seoulmarketprice.board.attachment.service.AttachmentService;
import com.seoul.market.seoulmarketprice.qna.dto.condition.QnaSearchCondition;
import com.seoul.market.seoulmarketprice.qna.dto.request.QnaCreateRequest;
import com.seoul.market.seoulmarketprice.qna.dto.request.QnaUpdateRequest;
import com.seoul.market.seoulmarketprice.qna.dto.response.QnaDetailResponse;
import com.seoul.market.seoulmarketprice.qna.dto.response.QnaFullDetailResponse;
import com.seoul.market.seoulmarketprice.qna.dto.response.QnaPageResponse;
import com.seoul.market.seoulmarketprice.qna.service.QnaService;
import com.seoul.market.seoulmarketprice.security.principal.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 프론트 화면에서 사용하는 공개 조회와 사용자 질문 관리 API를 제공한다. */
@Tag(name = "Q&A", description = "사용자 Q&A 게시판 API")
@RestController
@RequestMapping("/api/qnas")
@RequiredArgsConstructor
public class QnaController {
    /** Q&A 조회와 변경 업무를 처리하는 서비스이다. */
    private final QnaService qnaService;
    /** 공개 또는 작성자 전용 Q&A 첨부파일 업무를 처리한다. */
    private final AttachmentService attachmentService;

    /** 공개 질문을 키워드, 답변 상태와 페이지 조건으로 조회한다. */
    @Operation(summary = "공개 Q&A 목록 조회")
    @GetMapping
    public ResponseEntity<QnaPageResponse> getQnas(
            @Valid
            @ModelAttribute QnaSearchCondition condition) {
        return ResponseEntity.ok(qnaService.getPublicQnas(condition));
    }

    /** 현재 로그인 사용자가 작성한 질문 목록을 조회한다. */
    @Operation(summary = "내 Q&A 목록 조회")
    @GetMapping("/me")
    public ResponseEntity<QnaPageResponse> getMyQnas(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid
            @ModelAttribute QnaSearchCondition condition) {
        return ResponseEntity.ok(qnaService.getMyQnas(principal.memberId(), condition));
    }

    /** 공개 질문 또는 본인이 작성한 비공개 질문의 상세를 조회한다. */
    @Operation(summary = "Q&A 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<QnaDetailResponse> getQna(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(qnaService.getQna(id, principal == null ? null : principal.memberId()));
    }

    /** Q&A와 첨부파일을 상세 화면용 통합 응답으로 조회한다. */
    @Operation(summary = "Q&A 통합 상세 조회")
    @GetMapping("/{id}/full")
    public ResponseEntity<QnaFullDetailResponse> getFullQna(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Long userId = principal == null ? null : principal.memberId();
        QnaDetailResponse detail = qnaService.getQna(id, userId);
        return ResponseEntity.ok(new QnaFullDetailResponse(
                detail,
                attachmentService.list(AttachmentTargetType.QNA_BOARD, id)
        ));
    }

    /** 로그인 사용자의 식별자로 신규 질문을 등록한다. */
    @Operation(summary = "Q&A 질문 등록")
    @PostMapping
    public ResponseEntity<QnaDetailResponse> createQna(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody QnaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(qnaService.createQna(principal.memberId(), request));
    }

    /** 작성자가 자신의 질문을 부분 수정한다. */
    @Operation(summary = "본인 Q&A 질문 수정")
    @PatchMapping("/{id}")
    public ResponseEntity<QnaDetailResponse> updateQna(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody QnaUpdateRequest request) {
        return ResponseEntity.ok(qnaService.updateQna(id, principal.memberId(), request));
    }

    /** 작성자가 자신의 질문을 소프트 삭제한다. */
    @Operation(summary = "본인 Q&A 질문 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQna(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        qnaService.deleteQna(id, principal.memberId());
        return ResponseEntity.noContent().build();
    }

    /** 질문 작성자가 자신의 활성 Q&A에 파일을 첨부한다. */
    @PostMapping(path = "/{id}/attachments", consumes = "multipart/form-data")
    public ResponseEntity<List<AttachmentResponse>> uploadAttachments(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestPart("files") List<MultipartFile> files
    ) {
        qnaService.requireOwner(id, principal.memberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                attachmentService.upload(AttachmentTargetType.QNA_BOARD, id, files)
        );
    }

    /** 공개 질문 또는 작성자 본인의 비공개 질문 첨부파일 목록을 조회한다. */
    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<AttachmentResponse>> getAttachments(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Long userId = principal == null ? null : principal.memberId();
        qnaService.requireAccessible(id, userId);
        return ResponseEntity.ok(attachmentService.list(AttachmentTargetType.QNA_BOARD, id));
    }

    /** 접근 가능한 Q&A 첨부파일의 단기 다운로드 URL을 발급한다. */
    @GetMapping("/{id}/attachments/{attachmentId}/download")
    public ResponseEntity<AttachmentDownloadResponse> downloadAttachment(
            @PathVariable Long id, @PathVariable Long attachmentId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Long userId = principal == null ? null : principal.memberId();
        qnaService.requireAccessible(id, userId);
        return ResponseEntity.ok(attachmentService.download(
                AttachmentTargetType.QNA_BOARD, id, attachmentId
        ));
    }

    /** 질문 작성자가 자신의 첨부파일을 삭제한다. */
    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long id, @PathVariable Long attachmentId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        qnaService.requireOwner(id, principal.memberId());
        attachmentService.delete(AttachmentTargetType.QNA_BOARD, id, attachmentId);
        return ResponseEntity.noContent().build();
    }
}
