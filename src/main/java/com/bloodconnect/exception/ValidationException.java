package com.bloodconnect.exception;

/**
 * Exception thrown when user input fails business validation rules.
 */
public class ValidationException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message, 422);
    }
}
