package com.payment_service.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handle(Exception ex) {

        return ResponseEntity.badRequest().body(
                Map.of(
                        "error", ex.getMessage(),
                        "timestamp", LocalDateTime.now()
                )
        );
    }
}
