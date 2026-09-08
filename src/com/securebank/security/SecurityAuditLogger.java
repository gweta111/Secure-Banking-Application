package com.securebank.security;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Thread-safe audit logger that maintains an immutable record of security
 * and transactional events in data/audit.log.
 */
public class SecurityAuditLogger {
    private static final Path DEFAULT_LOG_PATH = Paths.get("data", "audit.log");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path logPath;

    public SecurityAuditLogger() {
        this(DEFAULT_LOG_PATH);
    }

    public SecurityAuditLogger(Path logPath) {
        this.logPath = logPath;
        initLogFile();
    }

    private void initLogFile() {
        try {
            if (logPath.getParent() != null && !Files.exists(logPath.getParent())) {
                Files.createDirectories(logPath.getParent());
            }
            if (!Files.exists(logPath)) {
                Files.createFile(logPath);
            }
        } catch (IOException e) {
            System.err.println("[SECURITY ALERT] Failed to initialize audit log: " + e.getMessage());
        }
    }

    public synchronized void log(String eventType, String actor, String details) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String logEntry = String.format("[%s] [%-18s] [Actor: %-15s] %s%n",
                timestamp,
                InputValidator.sanitizeForStorage(eventType),
                InputValidator.sanitizeForStorage(actor),
                InputValidator.sanitizeForStorage(details));

        try (BufferedWriter writer = Files.newBufferedWriter(logPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(logEntry);
        } catch (IOException e) {
            System.err.println("[AUDIT ERROR] Could not write to audit log: " + e.getMessage());
        }
    }

    public void logAuthSuccess(String username) {
        log("AUTH_SUCCESS", username, "User authenticated successfully.");
    }

    public void logAuthFailure(String username, int attemptCount) {
        log("AUTH_FAILURE", username, "Authentication attempt failed. Consecutive failure count: " + attemptCount);
    }

    public void logAccountLocked(String username) {
        log("ACCOUNT_LOCKED", username, "Account has been locked due to consecutive failed login attempts.");
    }

    public void logUserRegistered(String username, String userId) {
        log("USER_REGISTERED", username, "New user registered with UserID: " + userId);
    }

    public void logAccountCreated(String username, String accountNumber, String accountType) {
        log("ACCOUNT_CREATED", username, "Opened " + accountType + " account: " + accountNumber);
    }

    public void logTransaction(String username, String accountNumber, String type, String amount) {
        log("TRANSACTION", username, String.format("%s of $%s on account %s", type, amount, accountNumber));
    }
}
