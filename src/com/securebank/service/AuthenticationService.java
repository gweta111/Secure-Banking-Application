package com.securebank.service;

import com.securebank.model.BankingException;
import com.securebank.model.User;
import com.securebank.security.InputValidator;
import com.securebank.security.PasswordSecurity;
import com.securebank.security.SecurityAuditLogger;
import com.securebank.storage.DataStore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing user authentication, registration, session state,
 * and brute-force lockout protection.
 */
public class AuthenticationService {
    public static final int MAX_FAILED_ATTEMPTS = 3;

    private final DataStore dataStore;
    private final SecurityAuditLogger auditLogger;
    private User currentSessionUser;

    public AuthenticationService(DataStore dataStore, SecurityAuditLogger auditLogger) {
        this.dataStore = Objects.requireNonNull(dataStore, "DataStore cannot be null");
        this.auditLogger = Objects.requireNonNull(auditLogger, "SecurityAuditLogger cannot be null");
    }

    /**
     * Registers a new user with secure password hashing and input validation.
     */
    public synchronized User register(String username, String rawPassword) throws BankingException {
        if (!InputValidator.isValidUsername(username)) {
            throw new BankingException("Invalid username format. Must be 3-20 alphanumeric characters or underscores.");
        }

        String sanitizedUsername = username.trim();

        if (dataStore.findUserByUsername(sanitizedUsername).isPresent()) {
            throw new BankingException("Username '" + sanitizedUsername + "' is already registered. Please choose another.");
        }

        List<String> passwordViolations = PasswordSecurity.validatePasswordStrength(rawPassword);
        if (!passwordViolations.isEmpty()) {
            throw new BankingException("Password does not meet security criteria:\n - " +
                    String.join("\n - ", passwordViolations));
        }

        String salt = PasswordSecurity.generateSalt();
        String passwordHash = PasswordSecurity.hashPassword(rawPassword, salt);
        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        User newUser = new User(userId, sanitizedUsername, passwordHash, salt, 0, false, LocalDateTime.now());
        dataStore.saveUser(newUser);
        auditLogger.logUserRegistered(sanitizedUsername, userId);

        return newUser;
    }

    /**
     * Authenticates a user. Enforces lockout after consecutive failed attempts.
     */
    public synchronized User login(String username, String rawPassword) throws BankingException {
        if (username == null || username.trim().isEmpty() || rawPassword == null) {
            throw new BankingException("Username and password must not be empty.");
        }

        String sanitizedUsername = username.trim();
        Optional<User> userOpt = dataStore.findUserByUsername(sanitizedUsername);

        if (userOpt.isEmpty()) {
            // Constant-time dummy verification to mitigate user-enumeration timing attacks
            PasswordSecurity.verifyPassword(rawPassword,
                    "0000000000000000000000000000000000000000000000000000000000000000",
                    "0000000000000000");
            auditLogger.logAuthFailure(sanitizedUsername, 1);
            throw new BankingException("Invalid username or password.");
        }

        User user = userOpt.get();

        if (user.isLocked()) {
            auditLogger.log("ACCOUNT_LOCKED_ACCESS_ATTEMPT", sanitizedUsername, "Attempted login on locked account.");
            throw new BankingException("Account is locked due to multiple failed login attempts. Please contact bank administration.");
        }

        boolean passwordValid = PasswordSecurity.verifyPassword(rawPassword, user.getPasswordHash(), user.getSalt());

        if (!passwordValid) {
            user.incrementFailedAttempts();
            auditLogger.logAuthFailure(sanitizedUsername, user.getFailedLoginAttempts());

            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setLocked(true);
                dataStore.saveUser(user);
                auditLogger.logAccountLocked(sanitizedUsername);
                throw new BankingException("Invalid password. Security lockout triggered: Your account has been locked after " +
                        MAX_FAILED_ATTEMPTS + " consecutive failed attempts.");
            } else {
                dataStore.saveUser(user);
                int remaining = MAX_FAILED_ATTEMPTS - user.getFailedLoginAttempts();
                throw new BankingException("Invalid username or password. Remaining attempts before lockout: " + remaining);
            }
        }

        // Login successful: reset failed attempt counter
        user.resetFailedAttempts();
        dataStore.saveUser(user);
        auditLogger.logAuthSuccess(sanitizedUsername);

        this.currentSessionUser = user;
        return user;
    }

    public synchronized void logout() {
        if (currentSessionUser != null) {
            auditLogger.log("LOGOUT", currentSessionUser.getUsername(), "User logged out safely.");
            this.currentSessionUser = null;
        }
    }

    public synchronized User getCurrentSessionUser() {
        return currentSessionUser;
    }

    public synchronized boolean isAuthenticated() {
        return currentSessionUser != null;
    }

    /**
     * Administrative unlock helper (useful for support/testing).
     */
    public synchronized void unlockUser(String username) {
        Optional<User> userOpt = dataStore.findUserByUsername(username);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            u.setLocked(false);
            u.resetFailedAttempts();
            dataStore.saveUser(u);
            auditLogger.log("ACCOUNT_UNLOCKED", username, "Account administrative unlock performed.");
        }
    }
}
