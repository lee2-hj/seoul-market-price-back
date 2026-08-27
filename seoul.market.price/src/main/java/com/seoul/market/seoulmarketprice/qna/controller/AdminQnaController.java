package com.seoul.market.seoulmarketprice.qna.controller;

import com.seoul.market.seoulmarketprice.attachment.dto.AttachmentDownloadResponse;
import com.seoul.market.seoulmarketprice.attachment.dto.AttachmentResponse;
import com.seoul.market.seoulmarketprice.attachment.entity.AttachmentTargetType;
import com.seoul.market.seoulmarketprice.attachment.service.AttachmentService;
import com.seoul.market.seoulmarketprice.qna.dto.condition.AdminQnaSearchCondition;
import com.seoul.market.seoulmarketprice.qna.dto.request.QnaAnswerRequest;
import com.seoul.market.seoulmarketprice.qna.dto.request.QnaVisibilityRequest;
import com.seoul.market.seoulmarketprice.qna.dto.response.QnaDetailResponse;
import com.seoul.market.seoulmarketprice.qna.dto.response.QnaPageResponse;
import com.seoul.market.seoulmarketprice.qna.service.QnaService;
import com.seoul.market.seoulmarketprice.security.principal.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 백오피스 관리자가 Q&A를 조회하고 답변·공개·삭제 상태를 관리하는 API이다. */
@Tag(name = "관리자 Q&A", description = "백오피스 Q&A 관리 API")
@RestController
@RequestMapping("/api/admin/qnas")
@RequiredArgsConstructor
public class AdminQnaController {
    /** Q&A 관리자 업무를 처리하는 서비스이다. */
    private final QnaService qnaService;
    /** 비공개 질문까지 포함한 Q&A 첨부파일 관리 업무를 처리한다. */
    private final AttachmentService attachmentService;

    /** 관리자 검색 조건과 페이지 조건으로 활성 Q&A 목록을 조회한다. */
    @Operation(summary = "관리자 Q&A 목록 조회")
    @GetMapping
    public ResponseEntity<QnaPageResponse> getQnas(
            @Valid @ParameterObject
            @ModelAttribute AdminQnaSearchCondition condition) {
        return ResponseEntity.ok(qnaService.getAdminQnas(condition));
    }

    /** 관리자가 선택한 활성 Q&A의 상세를 조회한다. */
    @Operation(summary = "관리자 Q&A 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<QnaDetailResponse> getQna(@PathVariable Long id) {
        return ResponseEntity.ok(qnaService.getAdminQna(id));
    }

    /** 현재 관리자의 식별자로 답변을 등록하거나 수정한다. */
    @Operation(summary = "관리자 답변 등록 또는 수정")
    @PutMapping("/{id}/answer")
    public ResponseEntity<QnaDetailResponse> saveAnswer(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody QnaAnswerRequest request) {
        return ResponseEntity.ok(qnaService.saveAnswer(id, principal.memberId(), request));
    }

    /** 등록된 관리자 답변을 삭제하고 답변대기 상태로 되돌린다. */
    @Operation(summary = "관리자 답변 삭제")
    @DeleteMapping("/{id}/answer")
    public ResponseEntity<Void> deleteAnswer(@PathVariable Long id) {
        qnaService.deleteAnswer(id);
        return ResponseEntity.noContent().build();
    }

    /** 질문의 공개 또는 비공개 상태를 변경한다. */
    @Operation(summary = "Q&A 공개 여부 변경")
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<QnaDetailResponse> changeVisibility(@PathVariable Long id,
            @Valid @RequestBody QnaVisibilityRequest request) {
        return ResponseEntity.ok(qnaService.changeVisibility(id, request.publicQuestion()));
    }

    /** 관리자가 질문을 소프트 삭제한다. */
    @Operation(summary = "Q&A 관리자 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQna(@PathVariable Long id) {
        qnaService.deleteByAdmin(id);
        return ResponseEntity.noContent().build();
    }

    /** 관리자가 활성 Q&A에 파일을 첨부한다. */
    @PostMapping(path = "/{id}/attachments", consumes = "multipart/form-data")
    public ResponseEntity<List<AttachmentResponse>> uploadAttachments(
            @PathVariable Long id, @RequestPart("files") List<MultipartFile> files
    ) {
        qnaService.requireActive(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                attachmentService.upload(AttachmentTargetType.QNA_BOARD, id, files)
        );
    }

    /** 관리 권한으로 활성 Q&A의 첨부파일 목록을 조회한다. */
    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<AttachmentResponse>> getAttachments(@PathVariable Long id) {
        qnaService.requireActive(id);
        return ResponseEntity.ok(attachmentService.list(AttachmentTargetType.QNA_BOARD, id));
    }

    /** 관리 권한으로 Q&A 첨부파일 다운로드 URL을 발급한다. */
    @GetMapping("/{id}/attachments/{attachmentId}/download")
    public ResponseEntity<AttachmentDownloadResponse> downloadAttachment(
            @PathVariable Long id, @PathVariable Long attachmentId
    ) {
        qnaService.requireActive(id);
        return ResponseEntity.ok(attachmentService.download(
                AttachmentTargetType.QNA_BOARD, id, attachmentId
        ));
    }

    /** 관리 권한으로 Q&A 첨부파일을 삭제한다. */
    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long id, @PathVariable Long attachmentId
    ) {
        qnaService.requireActive(id);
        attachmentService.delete(AttachmentTargetType.QNA_BOARD, id, attachmentId);
        return ResponseEntity.noContent().build();
    }
}
