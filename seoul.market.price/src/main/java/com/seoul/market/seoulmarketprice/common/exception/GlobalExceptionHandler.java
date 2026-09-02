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
import com.seoul.market.seoulmarketprice.location.exception.SggNotFoundException;
import com.seoul.market.seoulmarketprice.qna.exception.QnaAccessDeniedException;
import com.seoul.market.seoulmarketprice.qna.exception.QnaNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * 프로젝트 전체 예외를 처리한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 존재하지 않는 자치구 코드로 행정동을 조회한 요청을 404 응답으로 변환한다. */
    @ExceptionHandler(SggNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSggNotFound(SggNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(String.valueOf(HttpStatus.NOT_FOUND.value()), exception.getMessage()));
    }

    /** 존재하지 않거나 이미 탈퇴한 회원 요청을 404 응답으로 변환한다. */
    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMemberNotFound(MemberNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(String.valueOf(HttpStatus.NOT_FOUND.value()), exception.getMessage()));
    }

    /** ModelAttribute 쿼리 파라미터 바인딩과 검증 실패를 400 응답으로 변환한다. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException exception) {
        String message = exception.getBindingResult().getFieldError() == null
                ? "요청 파라미터가 올바르지 않습니다."
                : exception.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(String.valueOf(HttpStatus.BAD_REQUEST.value()), message));
    }

    @ExceptionHandler(QnaNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleQnaNotFound(QnaNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(String.valueOf(HttpStatus.NOT_FOUND.value()), exception.getMessage()));
    }

    @ExceptionHandler(QnaAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleQnaAccessDenied(QnaAccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(String.valueOf(HttpStatus.FORBIDDEN.value()), exception.getMessage()));
    }

    /** 존재하지 않거나 공개되지 않은 FAQ 요청을 404로 변환한다. */
    @ExceptionHandler(FaqNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFaqNotFound(FaqNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(String.valueOf(HttpStatus.NOT_FOUND.value()), exception.getMessage()));
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommentNotFound(CommentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(String.valueOf(HttpStatus.NOT_FOUND.value()), exception.getMessage()));
    }

    @ExceptionHandler(CommentAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleCommentAccessDenied(CommentAccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(String.valueOf(HttpStatus.FORBIDDEN.value()), exception.getMessage()));
    }

    @ExceptionHandler(BoardNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBoardNotFound(BoardNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(String.valueOf(HttpStatus.NOT_FOUND.value()), exception.getMessage()));
    }

    @ExceptionHandler(BoardAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleBoardAccessDenied(BoardAccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(String.valueOf(HttpStatus.FORBIDDEN.value()), exception.getMessage()));
    }

    /** 존재하지 않거나 이미 삭제된 관리자 요청을 404로 변환한다. */
    @ExceptionHandler(AdminNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAdminNotFound(
            AdminNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(String.valueOf(HttpStatus.NOT_FOUND.value()), exception.getMessage()));
    }

    /** 안전 정책에 따라 거부된 관리자 삭제 요청을 409로 변환한다. */
    @ExceptionHandler(AdminDeletionException.class)
    public ResponseEntity<ErrorResponse> handleAdminDeletion(
            AdminDeletionException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(String.valueOf(HttpStatus.CONFLICT.value()), exception.getMessage()));
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
                        String.valueOf(HttpStatus.CONFLICT.value()),
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
        log.error("서버 내부 상태 오류가 발생했습니다.", exception);

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
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ErrorResponse(
                                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                                message
                        )
                );
    }

    /** fastApi 호출 시 응답으로 내려온 상태코드와 본문을 그대로 전달한다. */
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<byte[]> handleFastApiResponseError(
            RestClientResponseException exception
    ) {

        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        log.error(
                "fastApi 호출 중 오류 응답을 받았습니다. status={}, body={}",
                exception.getStatusCode(),
                exception.getResponseBodyAsString(),
                exception
        );

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(status);
        MediaType contentType = exception.getResponseHeaders() != null
                ? exception.getResponseHeaders().getContentType()
                : null;
        if (contentType != null) {
            responseBuilder.contentType(contentType);
        }

        return responseBuilder.body(exception.getResponseBodyAsByteArray());
    }

    /** fastApi 서버와 통신하지 못한 네트워크 오류를 발생 원인에 맞는 상태코드로 변환한다. */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleFastApiConnectionError(
            ResourceAccessException exception
    ) {

        HttpStatus status = exception.getCause() instanceof java.net.SocketTimeoutException
                ? HttpStatus.GATEWAY_TIMEOUT
                : HttpStatus.BAD_GATEWAY;

        return ResponseEntity
                .status(status)
                .body(
                        new ErrorResponse(
                                String.valueOf(status.value()),
                                "fastApi 서버와 통신할 수 없습니다."
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

        log.error("처리하지 못한 서버 예외가 발생했습니다.", exception);

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
