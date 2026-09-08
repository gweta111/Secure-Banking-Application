package com.securebank.model;

/**
 * Custom checked/runtime exception for banking and business logic failures.
 * Encapsulates domain-level error messages.
 */
public class BankingException extends RuntimeException {
    public BankingException(String message) {
        super(message);
    }

    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}
