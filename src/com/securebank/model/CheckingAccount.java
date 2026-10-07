package com.securebank.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * CheckingAccount represents a transactional account featuring an optional overdraft limit.
 * Demonstrates Inheritance and Polymorphic withdrawal rules.
 */
public class CheckingAccount extends Account {
    public static final BigDecimal DEFAULT_OVERDRAFT_LIMIT = new BigDecimal("200.00");

    private final BigDecimal overdraftLimit;

    public CheckingAccount(String accountNumber, String userId, BigDecimal initialBalance, LocalDateTime createdAt) {
        this(accountNumber, userId, initialBalance, createdAt, DEFAULT_OVERDRAFT_LIMIT);
    }

    public CheckingAccount(String accountNumber, String userId, BigDecimal initialBalance, LocalDateTime createdAt,
                           BigDecimal overdraftLimit) {
        super(accountNumber, userId, initialBalance, createdAt);
        this.overdraftLimit = (overdraftLimit != null ? overdraftLimit : DEFAULT_OVERDRAFT_LIMIT).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    @Override
    public synchronized void withdraw(BigDecimal amount) throws BankingException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Withdrawal amount must be strictly greater than zero.");
        }
        BigDecimal scaledAmount = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal projectedBalance = getBalance().subtract(scaledAmount);

        // Balance can go negative down to -overdraftLimit
        BigDecimal maxAllowedDeficit = overdraftLimit.negate();
        if (projectedBalance.compareTo(maxAllowedDeficit) < 0) {
            BigDecimal maxWithdrawal = getBalance().add(overdraftLimit);
            throw new BankingException(String.format(
                    "Withdrawal failed: Exceeds overdraft protection limit of $%.2f. Maximum available: $%.2f",
                    overdraftLimit, maxWithdrawal.max(BigDecimal.ZERO)));
        }

        setBalance(projectedBalance);
    }

    @Override
    public String getAccountType() {
        return "CHECKING";
    }

    @Override
    public String getAccountSummary() {
        return String.format("Overdraft Limit: $%.2f", overdraftLimit);
    }
}
