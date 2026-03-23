package com.inventory_service.exception;

/**
 * Exception thrown when request validation fails
 */
public class ValidationException extends RuntimeException {

    private String field;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}

