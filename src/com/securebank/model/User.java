package com.securebank.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a registered banking user.
 * Demonstrates encapsulation with private fields and controlled mutation.
 */
public class User {
    private final String userId;
    private final String username;
    private String passwordHash;
    private String salt;
    private int failedLoginAttempts;
    private boolean isLocked;
    private final LocalDateTime createdAt;

    public User(String userId, String username, String passwordHash, String salt,
                int failedLoginAttempts, boolean isLocked, LocalDateTime createdAt) {
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.username = Objects.requireNonNull(username, "Username cannot be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "Password hash cannot be null");
        this.salt = Objects.requireNonNull(salt, "Salt cannot be null");
        this.failedLoginAttempts = Math.max(0, failedLoginAttempts);
        this.isLocked = isLocked;
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash);
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = Objects.requireNonNull(salt);
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", isLocked=" + isLocked +
                ", failedLoginAttempts=" + failedLoginAttempts +
                ", createdAt=" + createdAt +
                '}';
    }
}
