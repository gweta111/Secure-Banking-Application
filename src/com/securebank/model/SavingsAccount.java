package com.securebank.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * SavingsAccount represents an interest-bearing account subject to a minimum balance constraint.
 * Demonstrates Inheritance and Polymorphic withdrawal rules.
 */
public class SavingsAccount extends Account {
    public static final BigDecimal DEFAULT_MIN_BALANCE = new BigDecimal("50.00");
    public static final BigDecimal DEFAULT_INTEREST_RATE = new BigDecimal("0.035"); // 3.5%

    private final BigDecimal minimumBalance;
    private final BigDecimal interestRate;

    public SavingsAccount(String accountNumber, String userId, BigDecimal initialBalance, LocalDateTime createdAt) {
        this(accountNumber, userId, initialBalance, createdAt, DEFAULT_MIN_BALANCE, DEFAULT_INTEREST_RATE);
    }

    public SavingsAccount(String accountNumber, String userId, BigDecimal initialBalance, LocalDateTime createdAt,
                          BigDecimal minimumBalance, BigDecimal interestRate) {
        super(accountNumber, userId, initialBalance, createdAt);
        this.minimumBalance = (minimumBalance != null ? minimumBalance : DEFAULT_MIN_BALANCE).setScale(2, RoundingMode.HALF_UP);
        this.interestRate = (interestRate != null ? interestRate : DEFAULT_INTEREST_RATE);

        if (initialBalance.compareTo(this.minimumBalance) < 0) {
            throw new BankingException("Initial deposit for a Savings Account must meet the minimum balance of $" + this.minimumBalance);
        }
    }

    public BigDecimal getMinimumBalance() {
        return minimumBalance;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    @Override
    public synchronized void withdraw(BigDecimal amount) throws BankingException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Withdrawal amount must be strictly greater than zero.");
        }
        BigDecimal scaledAmount = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal projectedBalance = getBalance().subtract(scaledAmount);

        if (projectedBalance.compareTo(minimumBalance) < 0) {
            throw new BankingException(String.format(
                    "Withdrawal failed: Savings Account requires a minimum balance of $%.2f. Available for withdrawal: $%.2f",
                    minimumBalance, getBalance().subtract(minimumBalance).max(BigDecimal.ZERO)));
        }

        setBalance(projectedBalance);
    }

    /**
     * Applies monthly interest to the balance.
     */
    public synchronized BigDecimal applyInterest() {
        BigDecimal monthlyRate = interestRate.divide(new BigDecimal("12"), 6, RoundingMode.HALF_UP);
        BigDecimal interestEarned = getBalance().multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        if (interestEarned.compareTo(BigDecimal.ZERO) > 0) {
            setBalance(getBalance().add(interestEarned));
        }
        return interestEarned;
    }

    @Override
    public String getAccountType() {
        return "SAVINGS";
    }

    @Override
    public String getAccountSummary() {
        return String.format("Type: SAVINGS | Balance: $%9.2f | Min Balance: $%7.2f | Annual Interest: %.2f%%",
                getBalance(), minimumBalance, interestRate.multiply(new BigDecimal("100")));
    }
}
