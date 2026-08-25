package com.seoul.market.seoulmarketprice.board.controller;

import com.seoul.market.seoulmarketprice.attachment.dto.AttachmentDownloadResponse;
import com.seoul.market.seoulmarketprice.attachment.dto.AttachmentResponse;
import com.seoul.market.seoulmarketprice.attachment.entity.AttachmentTargetType;
import com.seoul.market.seoulmarketprice.attachment.service.AttachmentService;
import com.seoul.market.seoulmarketprice.board.dto.request.AdminBoardUpdateRequest;
import com.seoul.market.seoulmarketprice.board.dto.request.NoticeCreateRequest;
import com.seoul.market.seoulmarketprice.board.dto.response.BoardDetailResponse;
import com.seoul.market.seoulmarketprice.board.dto.response.AdminBoardPageResponse;
import com.seoul.market.seoulmarketprice.board.dto.condition.BoardSearchCondition;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 관리자가 공지사항과 일반 게시글을 관리하는 API를 제공한다. */
@Tag(name = "관리자 게시판", description = "공지사항 및 게시글 관리 API")
@RestController
@RequestMapping("/api/admin/boards")
@RequiredArgsConstructor
public class AdminBoardController {

    /** 관리자 게시판 비즈니스 로직을 처리하는 서비스이다. */
    private final BoardService boardService;
    /** 공지사항을 포함한 관리자 게시판 첨부파일 업무를 처리한다. */
    private final AttachmentService attachmentService;

    /** 비공개 게시글을 포함한 관리자용 게시판 목록을 조회한다. */
    @Operation(summary = "관리자 게시판 목록 조회")
    @GetMapping
    public ResponseEntity<AdminBoardPageResponse> getBoards(
            @Valid @ParameterObject @org.springframework.web.bind.annotation.ModelAttribute
            BoardSearchCondition condition
    ) {
        return ResponseEntity.ok(boardService.getAdminBoards(condition));
    }

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

    /** 관리자가 활성 게시글 또는 공지사항에 파일을 첨부한다. */
    @PostMapping(path = "/{id}/attachments", consumes = "multipart/form-data")
    public ResponseEntity<List<AttachmentResponse>> uploadAttachments(
            @PathVariable Long id, @RequestPart("files") List<MultipartFile> files
    ) {
        boardService.requireActive(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                attachmentService.upload(AttachmentTargetType.BOARD, id, files)
        );
    }

    /** 공개 여부와 관계없이 활성 게시글의 첨부파일 목록을 조회한다. */
    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<AttachmentResponse>> getAttachments(@PathVariable Long id) {
        boardService.requireActive(id);
        return ResponseEntity.ok(attachmentService.list(AttachmentTargetType.BOARD, id));
    }

    /** 관리 권한으로 첨부파일의 단기 다운로드 URL을 발급한다. */
    @GetMapping("/{id}/attachments/{attachmentId}/download")
    public ResponseEntity<AttachmentDownloadResponse> downloadAttachment(
            @PathVariable Long id, @PathVariable Long attachmentId
    ) {
        boardService.requireActive(id);
        return ResponseEntity.ok(attachmentService.download(
                AttachmentTargetType.BOARD, id, attachmentId
        ));
    }

    /** 관리 권한으로 활성 게시글의 첨부파일을 삭제한다. */
    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long id, @PathVariable Long attachmentId
    ) {
        boardService.requireActive(id);
        attachmentService.delete(AttachmentTargetType.BOARD, id, attachmentId);
        return ResponseEntity.noContent().build();
    }
}
