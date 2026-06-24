package com.sol.ecopulse.exception;

import com.sol.ecopulse.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        List<ErrorResponse.FieldErrorResponse> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorResponse)
                .toList();

        ErrorResponse response = ErrorResponse.of(
                "INVALID_REQUEST",
                "요청 값이 올바르지 않습니다.",
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception) {
        List<ErrorResponse.FieldErrorResponse> errors = exception.getConstraintViolations()
                .stream()
                .map(this::toConstraintViolationErrorResponse)
                .toList();

        ErrorResponse response = ErrorResponse.of(
                "INVALID_REQUEST",
                "요청 파라미터가 올바르지 않습니다.",
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception
    ) {
        List<ErrorResponse.FieldErrorResponse> errors = exception.getParameterValidationResults()
                .stream()
                .flatMap(result -> result.getResolvableErrors()
                        .stream()
                        .map(error -> toParameterValidationErrorResponse(result, error)))
                .toList();

        ErrorResponse response = ErrorResponse.of(
                "INVALID_REQUEST",
                "요청 파라미터가 올바르지 않습니다.",
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException exception) {
        ErrorResponse response = ErrorResponse.of(
                "NOT_FOUND",
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_ARGUMENT",
                exception.getMessage()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        // 처리되지 않은 예외는 스택트레이스를 로깅하고, 내부 정보는 노출하지 않는다.
        log.error("처리되지 않은 예외가 발생했습니다.", exception);

        ErrorResponse response = ErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ErrorResponse.FieldErrorResponse toFieldErrorResponse(FieldError fieldError) {
        return new ErrorResponse.FieldErrorResponse(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
    }

    private ErrorResponse.FieldErrorResponse toConstraintViolationErrorResponse(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        String field = propertyPath.contains(".")
                ? propertyPath.substring(propertyPath.lastIndexOf(".") + 1)
                : propertyPath;

        return new ErrorResponse.FieldErrorResponse(
                field,
                violation.getMessage()
        );
    }

    private ErrorResponse.FieldErrorResponse toParameterValidationErrorResponse(
            ParameterValidationResult result,
            MessageSourceResolvable error
    ) {
        return new ErrorResponse.FieldErrorResponse(
                getParameterName(result),
                error.getDefaultMessage()
        );
    }

    private String getParameterName(ParameterValidationResult result) {
        MethodParameter methodParameter = result.getMethodParameter();

        if (methodParameter.getParameterName() != null) {
            return methodParameter.getParameterName();
        }

        return "parameter";
    }
}