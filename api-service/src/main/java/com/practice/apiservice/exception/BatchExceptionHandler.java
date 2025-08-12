package com.practice.apiservice.exception;

import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import com.practice.apiservice.utils.error.BatchConfigNotFoundException;

@RestControllerAdvice
public class BatchExceptionHandler {

    @ExceptionHandler({
            JobExecutionAlreadyRunningException.class,
            JobInstanceAlreadyCompleteException.class,
            JobRestartException.class
    })
    public ResponseEntity<?> jobConflict(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", "JOB_ALREADY_RUNNING", "message", ex.getMessage()));
    }

    @ExceptionHandler(BatchConfigNotFoundException.class)
    public ResponseEntity<?> configNotFound(BatchConfigNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "CONFIG_NOT_FOUND", "message", ex.getMessage()));
    }
}
