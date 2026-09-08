package com.securebank.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract base class representing a generic bank account.
 * Demonstrates Abstraction and Encapsulation by shielding direct balance modification
 * and requiring concrete implementations to define their own withdrawal/overdraft rules.
 */
public abstract class Account {
    private final String accountNumber;
    private final String userId;
    private BigDecimal balance;
    private final LocalDateTime createdAt;

    public Account(String accountNumber, String userId, BigDecimal initialBalance, LocalDateTime createdAt) {
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number cannot be null");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(initialBalance, "Initial balance cannot be null");
        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BankingException("Initial balance cannot be negative.");
        }
        this.balance = initialBalance.setScale(2, RoundingMode.HALF_UP);
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt timestamp cannot be null");
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    protected void setBalance(BigDecimal newBalance) {
        this.balance = newBalance.setScale(2, RoundingMode.HALF_UP);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Common deposit logic: ensures positive amounts and updates balance.
     */
    public synchronized void deposit(BigDecimal amount) throws BankingException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Deposit amount must be strictly greater than zero.");
        }
        BigDecimal scaledAmount = amount.setScale(2, RoundingMode.HALF_UP);
        setBalance(this.balance.add(scaledAmount));
    }

    /**
     * Abstract method enforcing polymorphic withdrawal behavior.
     * Concrete account types must implement specific overdraft or minimum balance logic.
     */
    public abstract void withdraw(BigDecimal amount) throws BankingException;

    /**
     * Returns the account type name (e.g., SAVINGS, CHECKING).
     */
    public abstract String getAccountType();

    /**
     * Returns a formatted summary of account specifics (rates, limits, etc.).
     */
    public abstract String getAccountSummary();

    @Override
    public String toString() {
        return String.format("[%s] Account: %s | Balance: $%9.2f | Owner: %s",
                getAccountType(), accountNumber, balance, userId);
    }
}
