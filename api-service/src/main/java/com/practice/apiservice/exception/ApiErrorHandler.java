package com.practice.apiservice.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.practice.apiservice.utils.error.FileTooLargeException;
import com.practice.apiservice.utils.error.ResourceNotFoundException;

@RestControllerAdvice
public class ApiErrorHandler {
    public record FieldItem(String field, String message) {}
    public record ApiError(String code, String message, List<FieldItem> fields) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> onValidation(MethodArgumentNotValidException ex) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldItem(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_ERROR", "Invalid payload", fields));
    }

    public static ResponseEntity<ApiError> badRequestFrom(BindingResult br) {
        var fields = br.getFieldErrors().stream()
                .map(fe -> new FieldItem(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_ERROR", "Invalid payload", fields));
    }

    @ExceptionHandler(FileTooLargeException.class)
    ResponseEntity<ApiError> onTooLarge(FileTooLargeException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError("FILE_TOO_LARGE",
                        "Max " + ex.getMax() + " bytes, recibió " + ex.getActual(), List.of()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> onNotFound(com.practice.apiservice.utils.error.ResourceNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ApiError("NOT_FOUND", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> onIllegalArgument(IllegalArgumentException ex) {
        List<FieldItem> fields = List.of();
        
        String message = ex.getMessage();
        if (message != null) {
            if (message.contains("originalFilename is blank")) {
                fields = List.of(new FieldItem("originalFilename", "must not be blank"));
            } else if (message.contains("checksumSha256 must contain exactly 64 hexadecimal characters")) {
                fields = List.of(new FieldItem("checksumSha256", "must be a valid SHA-256 hash"));
            } else if (message.contains("storagePath is blank")) {
                fields = List.of(new FieldItem("storagePath", "must not be blank"));
            } else if (message.contains("sizeBytes must be > 0")) {
                fields = List.of(new FieldItem("sizeBytes", "must be greater than 0"));
            }
        }

        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_ERROR", "Validation failed", fields));
    }
}
