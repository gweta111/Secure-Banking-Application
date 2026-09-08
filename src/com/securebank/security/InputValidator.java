package com.securebank.security;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Handles validation and sanitization of all user inputs.
 * Defends against delimiter injection, command injection, and invalid financial data.
 */
public final class InputValidator {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern ACCOUNT_NUM_PATTERN = Pattern.compile("^[A-Z0-9-]{6,20}$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^(0|[1-9][0-9]*)(\\.[0-9]{1,2})?$");

    private InputValidator() {
        // Utility class
    }

    /**
     * Validates that a username consists only of alphanumeric characters and underscores,
     * between 3 and 20 characters in length.
     */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    /**
     * Validates that an account number matches the expected alphanumeric structure.
     */
    public static boolean isValidAccountNumber(String accountNumber) {
        return accountNumber != null && ACCOUNT_NUM_PATTERN.matcher(accountNumber.trim()).matches();
    }

    /**
     * Validates that the input represents a positive currency amount with at most two decimal places.
     */
    public static boolean isValidCurrencyString(String input) {
        if (input == null) {
            return false;
        }
        String trimmed = input.trim();
        if (!AMOUNT_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        try {
            BigDecimal amount = new BigDecimal(trimmed);
            return amount.compareTo(BigDecimal.ZERO) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Parses and returns a valid positive BigDecimal from user input.
     * Throws IllegalArgumentException if invalid.
     */
    public static BigDecimal parseAmount(String input) {
        if (!isValidCurrencyString(input)) {
            throw new IllegalArgumentException("Invalid amount. Must be a positive number with up to 2 decimal places (e.g., 100.50).");
        }
        return new BigDecimal(input.trim());
    }

    /**
     * Sanitizes strings destined for file storage by removing delimiter characters ('|')
     * and newline/carriage-return characters to prevent text injection.
     */
    public static String sanitizeForStorage(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("|", "/")
                    .replace("\r", "")
                    .replace("\n", " ")
                    .trim();
    }
}
