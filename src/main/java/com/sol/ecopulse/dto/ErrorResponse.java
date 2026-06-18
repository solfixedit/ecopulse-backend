package com.sol.ecopulse.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        List<FieldErrorResponse> errors,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of(), LocalDateTime.now());
    }

    public static ErrorResponse of(String code, String message, List<FieldErrorResponse> errors) {
        return new ErrorResponse(code, message, errors, LocalDateTime.now());
    }

    public record FieldErrorResponse(
            String field,
            String message
    ) {}
}
