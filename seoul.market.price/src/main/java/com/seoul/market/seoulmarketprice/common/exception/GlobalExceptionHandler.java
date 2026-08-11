package com.seoul.market.seoulmarketprice.common.exception;

import com.seoul.market.seoulmarketprice.common.dto.ErrorResponse;
import com.seoul.market.seoulmarketprice.board.exception.BoardAccessDeniedException;
import com.seoul.market.seoulmarketprice.board.exception.BoardNotFoundException;
import com.seoul.market.seoulmarketprice.comment.exception.CommentAccessDeniedException;
import com.seoul.market.seoulmarketprice.comment.exception.CommentNotFoundException;
import com.seoul.market.seoulmarketprice.faq.exception.FaqNotFoundException;
import com.seoul.market.seoulmarketprice.member.exception.DuplicateMemberException;
import com.seoul.market.seoulmarketprice.member.exception.DuplicateAdminException;
import com.seoul.market.seoulmarketprice.member.exception.AdminDeletionException;
import com.seoul.market.seoulmarketprice.member.exception.AdminNotFoundException;
import com.seoul.market.seoulmarketprice.member.exception.MemberNotFoundException;
import com.seoul.market.seoulmarketprice.qna.exception.QnaAccessDeniedException;
import com.seoul.market.seoulmarketprice.qna.exception.QnaNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 프로젝트 전체 예외를 처리한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 존재하지 않거나 이미 탈퇴한 회원 요청을 404 응답으로 변환한다. */
    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMemberNotFound(MemberNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("MEMBER-001", exception.getMessage()));
    }

    /** ModelAttribute 쿼리 파라미터 바인딩과 검증 실패를 400 응답으로 변환한다. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException exception) {
        String message = exception.getBindingResult().getFieldError() == null
                ? "요청 파라미터가 올바르지 않습니다."
                : exception.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALID-001", message));
    }

    @ExceptionHandler(QnaNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleQnaNotFound(QnaNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("QNA-001", exception.getMessage()));
    }

    @ExceptionHandler(QnaAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleQnaAccessDenied(QnaAccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("QNA-002", exception.getMessage()));
    }

    /** 존재하지 않거나 공개되지 않은 FAQ 요청을 404로 변환한다. */
    @ExceptionHandler(FaqNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFaqNotFound(FaqNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("FAQ-001", exception.getMessage()));
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommentNotFound(CommentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("COMMENT-001", exception.getMessage()));
    }

    @ExceptionHandler(CommentAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleCommentAccessDenied(CommentAccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("COMMENT-002", exception.getMessage()));
    }

    @ExceptionHandler(BoardNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBoardNotFound(BoardNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("BOARD-001", exception.getMessage()));
    }

    @ExceptionHandler(BoardAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleBoardAccessDenied(BoardAccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("BOARD-002", exception.getMessage()));
    }

    /** 존재하지 않거나 이미 삭제된 관리자 요청을 404로 변환한다. */
    @ExceptionHandler(AdminNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAdminNotFound(
            AdminNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("ADMIN-002", exception.getMessage()));
    }

    /** 안전 정책에 따라 거부된 관리자 삭제 요청을 409로 변환한다. */
    @ExceptionHandler(AdminDeletionException.class)
    public ResponseEntity<ErrorResponse> handleAdminDeletion(
            AdminDeletionException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("ADMIN-003", exception.getMessage()));
    }

    /**
     * 관리자 아이디 중복 오류를 처리한다.
     */
    @ExceptionHandler(DuplicateAdminException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateAdmin(
            DuplicateAdminException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "ADMIN-001",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(DuplicateMemberException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateMember(
            DuplicateMemberException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        String.valueOf(HttpStatus.CONFLICT.value()),
                        exception.getMessage()
                ));
    }

    /**
     * 잘못된 요청 처리.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception
    ) {

        HttpStatus status = HttpStatus.BAD_REQUEST;

        return ResponseEntity
                .status(status)
                .body(
                        new ErrorResponse(
                                String.valueOf(status.value()),
                                exception.getMessage()
                        )
                );
    }

    /**
     * 서버 내부 오류 처리.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException exception
    ) {

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        return ResponseEntity
                .status(status)
                .body(
                        new ErrorResponse(
                                String.valueOf(status.value()),
                                exception.getMessage()
                        )
                );
    }

    /**
     * DTO 검증 실패 처리.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception
    ) {

        String message = exception
                .getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        return ResponseEntity
                .badRequest()
                .body(
                        new ErrorResponse(
                                "VALID-001",
                                message
                        )
                );
    }

    /**
     * 처리하지 못한 예외.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception exception
    ) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        new ErrorResponse(
                                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                                "예기치 못한 오류가 발생하였습니다."
                        )
                );
    }
}
