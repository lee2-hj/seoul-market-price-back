package com.seoul.market.seoulmarketprice.common.exception;

import com.seoul.market.seoulmarketprice.common.dto.ErrorResponse;
import com.seoul.market.seoulmarketprice.member.exception.DuplicateMemberException;
import com.seoul.market.seoulmarketprice.member.exception.DuplicateAdminException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 프로젝트 전체 예외를 처리한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

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
                        "MEMBER-001",
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

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ErrorResponse(
                                "AUTH-001",
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

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        new ErrorResponse(
                                "AUTH-002",
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
                                "SERVER-001",
                                "서버 오류가 발생했습니다."
                        )
                );
    }
}
