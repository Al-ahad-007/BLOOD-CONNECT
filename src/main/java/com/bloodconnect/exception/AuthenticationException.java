package com.bloodconnect.exception;

/**
 * Exception thrown when login, credential verification or authorization fails.
 */
public class AuthenticationException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message, 401);
    }
}
