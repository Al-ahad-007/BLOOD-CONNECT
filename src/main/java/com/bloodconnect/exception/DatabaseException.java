package com.bloodconnect.exception;

/**
 * Exception thrown when a database or JDBC error occurs.
 */
public class DatabaseException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    public DatabaseException(String message) {
        super(message, 500);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
