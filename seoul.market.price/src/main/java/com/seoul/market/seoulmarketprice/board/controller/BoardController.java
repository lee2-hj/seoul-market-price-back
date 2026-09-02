package com.seoul.market.seoulmarketprice.board.controller;

import com.seoul.market.seoulmarketprice.board.attachment.dto.AttachmentDownloadResponse;
import com.seoul.market.seoulmarketprice.board.attachment.dto.AttachmentResponse;
import com.seoul.market.seoulmarketprice.board.attachment.entity.AttachmentTargetType;
import com.seoul.market.seoulmarketprice.board.attachment.service.AttachmentService;
import com.seoul.market.seoulmarketprice.board.dto.condition.BoardSearchCondition;
import com.seoul.market.seoulmarketprice.board.dto.request.BoardCreateRequest;
import com.seoul.market.seoulmarketprice.board.dto.request.BoardUpdateRequest;
import com.seoul.market.seoulmarketprice.board.dto.response.BoardDetailResponse;
import com.seoul.market.seoulmarketprice.board.dto.response.BoardFullDetailResponse;
import com.seoul.market.seoulmarketprice.board.dto.response.BoardPageResponse;
import com.seoul.market.seoulmarketprice.board.service.BoardService;
import com.seoul.market.seoulmarketprice.board.comment.service.CommentService;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 일반 사용자가 게시글을 조회하고 작성·수정·삭제하는 API를 제공한다. */
@Tag(name = "일반 게시판", description = "일반 게시글 및 공지사항 조회 API")
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    /** 일반 게시판 비즈니스 로직을 처리하는 서비스이다. */
    private final BoardService boardService;

    /** 일반 게시판 첨부파일의 저장과 조회를 처리한다. */
    private final AttachmentService attachmentService;

    private final CommentService commentService;

    /** 공개 게시글을 공지 우선, 최신순으로 조회한다. */
    @Operation(summary = "게시글 목록 조회")
    @GetMapping
    public ResponseEntity<BoardPageResponse> getBoards(
            @Valid
            @ModelAttribute BoardSearchCondition condition
    ) {
        return ResponseEntity.ok(
                boardService.getBoards(condition)
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

    @Operation(summary = "내 게시글 목록 조회")
    @GetMapping("/me")
    public ResponseEntity<BoardPageResponse> getMyBoards(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @ModelAttribute BoardSearchCondition condition
    ) {
        return ResponseEntity.ok(boardService.getMyBoards(principal.memberId(), condition));
    }

    /** 게시글, 댓글, 첨부파일을 상세 화면용 통합 응답으로 조회한다. */
    @Operation(summary = "게시글 통합 상세 조회")
    @GetMapping("/{id}/full")
    public ResponseEntity<BoardFullDetailResponse> getFullBoard(@PathVariable Long id) {
        BoardDetailResponse detail = boardService.getBoard(id);
        return ResponseEntity.ok(new BoardFullDetailResponse(
                detail,
                commentService.getComments(id),
                attachmentService.list(AttachmentTargetType.BOARD, id)
        ));
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
                principal.userId(),
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

    /** 작성자 본인의 게시글에 검증된 파일을 최대 허용 개수까지 첨부한다. */
    @PostMapping(path = "/{id}/attachments", consumes = "multipart/form-data")
    public ResponseEntity<List<AttachmentResponse>> uploadAttachments(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestPart("files") List<MultipartFile> files
    ) {
        boardService.requireOwner(id, principal.memberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                attachmentService.upload(AttachmentTargetType.BOARD, id, files)
        );
    }

    /** 공개 게시글에 등록된 활성 첨부파일 목록을 조회한다. */
    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<AttachmentResponse>> getAttachments(@PathVariable Long id) {
        boardService.requirePublicAccess(id);
        return ResponseEntity.ok(attachmentService.list(AttachmentTargetType.BOARD, id));
    }

    /** 공개 게시글 첨부파일의 단기 MinIO 다운로드 URL을 발급한다. */
    @GetMapping("/{id}/attachments/{attachmentId}/download")
    public ResponseEntity<AttachmentDownloadResponse> downloadAttachment(
            @PathVariable Long id, @PathVariable Long attachmentId
    ) {
        boardService.requirePublicAccess(id);
        return ResponseEntity.ok(attachmentService.download(
                AttachmentTargetType.BOARD, id, attachmentId
        ));
    }

    /** 작성자 본인의 첨부파일을 MinIO와 활성 목록에서 제거한다. */
    /** 작성자 본인의 첨부파일을 MinIO와 활성 목록에서 제거한다. */
    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long id, @PathVariable Long attachmentId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        boardService.requireOwner(id, principal.memberId());
        attachmentService.delete(AttachmentTargetType.BOARD, id, attachmentId);
        return ResponseEntity.noContent().build();
    }
}
