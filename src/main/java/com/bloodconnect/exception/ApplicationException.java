package com.bloodconnect.exception;

/**
 * Base custom exception for all BloodConnect application-level errors.
 */
public class ApplicationException extends Exception {
    private static final long serialVersionUID = 1L;
    private int statusCode = 400;

    public ApplicationException(String message) {
        super(message);
    }

    public ApplicationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public int getStatusCode() {
        return statusCode;
    }
}
