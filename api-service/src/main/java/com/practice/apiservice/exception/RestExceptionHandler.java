package com.practice.apiservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import com.practice.apiservice.utils.error.FileTooLargeException;
import com.practice.apiservice.utils.error.ResourceNotFoundException;

@RestControllerAdvice
public class RestExceptionHandler {
    public record FieldItem(String field, String message) {}
    public record ErrorResponse(
            Instant timestamp,
            String  path,
            String  code,
            String  message,
            List<FieldItem> fields,
            String  traceId
    ){}

    public record ApiError(String code, String message, List<FieldItem> fields) {}

    /* ---- mapeos 400 ---- */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> onValidation(Exception ex, HttpServletRequest req) {
        var binding = (ex instanceof MethodArgumentNotValidException manv)
                ? manv.getBindingResult()
                : ((BindException) ex).getBindingResult();

        var fields = binding.getFieldErrors().stream()
                .map(fe -> new FieldItem(fe.getField(), defaultMsg(fe)))
                .toList();

        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Invalid request", req, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> onConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        var fields = ex.getConstraintViolations().stream()
                .map(v -> new FieldItem(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Invalid request", req, fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> onMalformed(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_JSON",
                "Malformed JSON", req, List.of());
    }

    /* ---- negocio (400/404) ---- */

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> onIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
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

        return build(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION",
                safeMessage(ex.getMessage()), req, fields);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> onNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND",
                safeMessage(ex.getMessage()), req, List.of());
    }

    /* ---- infraestructura (503) ---- */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> onInfra(DataAccessException ex, HttpServletRequest req) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                "Database unavailable", req, List.of());
    }

    /* ---- catch-all (500) ---- */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> onUnexpected(Exception ex, HttpServletRequest req) {
        // Log real a nivel ERROR en tu logger/observabilidad (omito aquí por brevedad)
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "UNEXPECTED_ERROR",
                "Unexpected server error", req, List.of());
    }

    public static ResponseEntity<ApiError> badRequestFrom(BindingResult br) {
        var fields = br.getFieldErrors().stream()
                .map(fe -> new FieldItem(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_ERROR", "Invalid payload", fields));
    }

    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<ErrorResponse> onTooLarge(FileTooLargeException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE",
                safeMessage(ex.getMessage()), req, List.of());
    }

    /* ---- helpers ---- */
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                HttpServletRequest req, List<FieldItem> fields) {
        String traceId = MDC.get("traceId"); // si usas MDC/Sleuth/Observability
        var body = new ErrorResponse(
                Instant.now(),
                req.getRequestURI(),
                code,
                message,
                fields == null ? List.of() : fields,
                traceId
        );
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private static String defaultMsg(FieldError fe) {
        return (fe.getDefaultMessage() == null || fe.getDefaultMessage().isBlank())
                ? "invalid value" : fe.getDefaultMessage();
    }

    private static String safeMessage(String m) {
        return (m == null || m.isBlank()) ? "Operation failed" : m;
    }

}
