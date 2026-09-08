package com.securebank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents an immutable financial transaction record.
 * Demonstrates encapsulation and data integrity.
 */
public class Transaction {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String transactionId;
    private final String accountNumber;
    private final TransactionType type;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;
    private final BigDecimal resultingBalance;
    private final String description;

    public Transaction(String transactionId, String accountNumber, TransactionType type,
                       BigDecimal amount, LocalDateTime timestamp, BigDecimal resultingBalance,
                       String description) {
        this.transactionId = Objects.requireNonNull(transactionId, "Transaction ID cannot be null");
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number cannot be null");
        this.type = Objects.requireNonNull(type, "Transaction type cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.resultingBalance = Objects.requireNonNull(resultingBalance, "Resulting balance cannot be null");
        this.description = description == null ? "" : description;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public BigDecimal getResultingBalance() {
        return resultingBalance;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | Type: %-12s | Amount: $%9.2f | Balance: $%9.2f | Note: %s",
                timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                transactionId,
                type,
                amount,
                resultingBalance,
                description);
    }
}
