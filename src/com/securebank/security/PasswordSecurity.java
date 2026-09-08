package com.securebank.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Handles cryptographic operations for password security.
 * Demonstrates secure password hashing using SHA-256 with salt,
 * constant-time verification to prevent timing attacks,
 * and robust password policy enforcement.
 */
public final class PasswordSecurity {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SALT_BYTES = 16;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private PasswordSecurity() {
        // Utility class
    }

    /**
     * Generates a cryptographically secure random salt encoded in hex.
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    /**
     * Hashes a plaintext password with the provided salt using SHA-256.
     */
    public static String hashPassword(String password, String salt) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password and salt cannot be null.");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashedBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in current JRE", e);
        }
    }

    /**
     * Verifies whether a candidate plaintext password matches the stored hash and salt.
     * Uses MessageDigest.isEqual for constant-time comparison to prevent side-channel timing attacks.
     */
    public static boolean verifyPassword(String candidatePassword, String storedHash, String salt) {
        if (candidatePassword == null || storedHash == null || salt == null) {
            return false;
        }
        String candidateHash = hashPassword(candidatePassword, salt);
        byte[] candidateBytes = candidateHash.getBytes(StandardCharsets.UTF_8);
        byte[] storedBytes = storedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(candidateBytes, storedBytes);
    }

    /**
     * Validates password complexity against modern security standards.
     * Returns a list of violated criteria (empty list if password is fully compliant).
     */
    public static List<String> validatePasswordStrength(String password) {
        List<String> violations = new ArrayList<>();
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            violations.add("At least " + MIN_PASSWORD_LENGTH + " characters long");
        }
        if (password == null || !password.chars().anyMatch(Character::isUpperCase)) {
            violations.add("At least one uppercase letter (A-Z)");
        }
        if (password == null || !password.chars().anyMatch(Character::isLowerCase)) {
            violations.add("At least one lowercase letter (a-z)");
        }
        if (password == null || !password.chars().anyMatch(Character::isDigit)) {
            violations.add("At least one numeric digit (0-9)");
        }
        if (password == null || !password.chars().anyMatch(ch -> "!@#$%^&*()-_=+[]{}|;:,.<>?/".indexOf(ch) >= 0)) {
            violations.add("At least one special character (!@#$%^&*()-_=+[]{}|;:,.<>?/)");
        }
        return violations;
    }
}
