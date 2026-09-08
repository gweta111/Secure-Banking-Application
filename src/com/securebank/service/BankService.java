package com.securebank.service;

import com.securebank.model.Account;
import com.securebank.model.BankingException;
import com.securebank.model.CheckingAccount;
import com.securebank.model.SavingsAccount;
import com.securebank.model.Transaction;
import com.securebank.model.TransactionType;
import com.securebank.model.User;
import com.securebank.security.InputValidator;
import com.securebank.security.SecurityAuditLogger;
import com.securebank.storage.DataStore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service managing core banking operations: account creation,
 * deposits, withdrawals, transfers, and transaction auditing.
 * Demonstrates business logic encapsulation and authorization enforcement.
 */
public class BankService {
    private final DataStore dataStore;
    private final SecurityAuditLogger auditLogger;
    private final AtomicInteger accountSequence = new AtomicInteger(1000);

    public BankService(DataStore dataStore, SecurityAuditLogger auditLogger) {
        this.dataStore = Objects.requireNonNull(dataStore, "DataStore cannot be null");
        this.auditLogger = Objects.requireNonNull(auditLogger, "SecurityAuditLogger cannot be null");
        initSequence();
    }

    private void initSequence() {
        int maxSeq = 1000;
        for (Account acc : dataStore.getAllAccounts()) {
            String accNum = acc.getAccountNumber();
            String[] parts = accNum.split("-");
            if (parts.length == 3) {
                try {
                    int seq = Integer.parseInt(parts[2]);
                    if (seq > maxSeq) {
                        maxSeq = seq;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        accountSequence.set(maxSeq);
    }

    /**
     * Creates a new bank account (SAVINGS or CHECKING) for an authenticated user.
     */
    public synchronized Account createAccount(User user, String accountType, BigDecimal initialDeposit) throws BankingException {
        if (user == null) {
            throw new BankingException("Authentication required to open an account.");
        }
        if (accountType == null || (!accountType.equalsIgnoreCase("SAVINGS") && !accountType.equalsIgnoreCase("CHECKING"))) {
            throw new BankingException("Invalid account type. Choose either SAVINGS or CHECKING.");
        }

        BigDecimal depositAmount = initialDeposit != null ? initialDeposit.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        String typeUpper = accountType.toUpperCase();
        int nextNum = accountSequence.incrementAndGet();
        String prefix = typeUpper.equals("SAVINGS") ? "SAV" : "CHK";
        String accountNumber = String.format("ACC-%s-%04d", prefix, nextNum);

        Account newAccount;
        LocalDateTime now = LocalDateTime.now();

        if (typeUpper.equals("SAVINGS")) {
            newAccount = new SavingsAccount(accountNumber, user.getUserId(), depositAmount, now);
        } else {
            newAccount = new CheckingAccount(accountNumber, user.getUserId(), depositAmount, now);
        }

        dataStore.saveAccount(newAccount);
        auditLogger.logAccountCreated(user.getUsername(), accountNumber, typeUpper);

        if (depositAmount.compareTo(BigDecimal.ZERO) > 0) {
            Transaction initialTx = new Transaction(
                    "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    accountNumber,
                    TransactionType.DEPOSIT,
                    depositAmount,
                    now,
                    newAccount.getBalance(),
                    "Initial opening deposit"
            );
            dataStore.recordTransaction(initialTx);
        }

        return newAccount;
    }

    /**
     * Retrieves all accounts owned by a given user.
     */
    public List<Account> getUserAccounts(User user) {
        if (user == null) {
            throw new BankingException("User session is invalid.");
        }
        return dataStore.findAccountsByUserId(user.getUserId());
    }

    /**
     * Retrieves an account, enforcing that the requesting user is the rightful owner.
     */
    public Account getVerifiedAccount(User user, String accountNumber) throws BankingException {
        if (user == null) {
            throw new BankingException("Authentication required.");
        }
        if (!InputValidator.isValidAccountNumber(accountNumber)) {
            throw new BankingException("Invalid account number format.");
        }

        Optional<Account> accountOpt = dataStore.findAccountByNumber(accountNumber);
        if (accountOpt.isEmpty()) {
            throw new BankingException("Account " + accountNumber + " does not exist.");
        }

        Account account = accountOpt.get();
        if (!account.getUserId().equalsIgnoreCase(user.getUserId())) {
            auditLogger.log("UNAUTHORIZED_ACCESS", user.getUsername(), "Attempted unauthorized access to account: " + accountNumber);
            throw new BankingException("Access denied: You do not own account " + accountNumber);
        }

        return account;
    }

    /**
     * Deposits funds into an account owned by the user.
     */
    public synchronized Transaction deposit(User user, String accountNumber, BigDecimal amount) throws BankingException {
        Account account = getVerifiedAccount(user, accountNumber);

        account.deposit(amount);
        dataStore.saveAccount(account);

        Transaction tx = new Transaction(
                "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                account.getAccountNumber(),
                TransactionType.DEPOSIT,
                amount.setScale(2, RoundingMode.HALF_UP),
                LocalDateTime.now(),
                account.getBalance(),
                "Cash deposit"
        );

        dataStore.recordTransaction(tx);
        auditLogger.logTransaction(user.getUsername(), accountNumber, "DEPOSIT", amount.toPlainString());
        return tx;
    }

    /**
     * Withdraws funds from an account owned by the user.
     * Polymorphically applies Savings vs Checking rules.
     */
    public synchronized Transaction withdraw(User user, String accountNumber, BigDecimal amount) throws BankingException {
        Account account = getVerifiedAccount(user, accountNumber);

        account.withdraw(amount);
        dataStore.saveAccount(account);

        Transaction tx = new Transaction(
                "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                account.getAccountNumber(),
                TransactionType.WITHDRAWAL,
                amount.setScale(2, RoundingMode.HALF_UP),
                LocalDateTime.now(),
                account.getBalance(),
                "Cash withdrawal"
        );

        dataStore.recordTransaction(tx);
        auditLogger.logTransaction(user.getUsername(), accountNumber, "WITHDRAWAL", amount.toPlainString());
        return tx;
    }

    /**
     * Performs an atomic funds transfer between two accounts.
     */
    public synchronized void transfer(User user, String fromAccountNum, String toAccountNum, BigDecimal amount) throws BankingException {
        if (fromAccountNum == null || toAccountNum == null) {
            throw new BankingException("Source and destination accounts must be specified.");
        }
        if (fromAccountNum.trim().equalsIgnoreCase(toAccountNum.trim())) {
            throw new BankingException("Cannot transfer funds to the same account.");
        }

        Account fromAccount = getVerifiedAccount(user, fromAccountNum);

        Optional<Account> toAccountOpt = dataStore.findAccountByNumber(toAccountNum);
        if (toAccountOpt.isEmpty()) {
            throw new BankingException("Destination account '" + toAccountNum + "' does not exist.");
        }
        Account toAccount = toAccountOpt.get();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Transfer amount must be strictly greater than zero.");
        }

        BigDecimal scaledAmount = amount.setScale(2, RoundingMode.HALF_UP);

        // Perform withdrawal from source (polymorphic overdraft/min balance check occurs here)
        fromAccount.withdraw(scaledAmount);
        // Deposit into target
        toAccount.deposit(scaledAmount);

        // Save both accounts atomically
        dataStore.saveAccount(fromAccount);
        dataStore.saveAccount(toAccount);

        LocalDateTime now = LocalDateTime.now();
        String txIdOut = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String txIdIn = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Transaction outTx = new Transaction(
                txIdOut,
                fromAccount.getAccountNumber(),
                TransactionType.TRANSFER_OUT,
                scaledAmount,
                now,
                fromAccount.getBalance(),
                "Transfer to " + toAccount.getAccountNumber()
        );

        Transaction inTx = new Transaction(
                txIdIn,
                toAccount.getAccountNumber(),
                TransactionType.TRANSFER_IN,
                scaledAmount,
                now,
                toAccount.getBalance(),
                "Transfer from " + fromAccount.getAccountNumber()
        );

        dataStore.recordTransaction(outTx);
        dataStore.recordTransaction(inTx);

        auditLogger.log("TRANSFER_EXECUTED", user.getUsername(),
                String.format("Transferred $%s from %s to %s", scaledAmount.toPlainString(), fromAccountNum, toAccountNum));
    }

    /**
     * Retrieves transaction history for an account owned by the user.
     */
    public List<Transaction> getTransactionHistory(User user, String accountNumber) throws BankingException {
        Account account = getVerifiedAccount(user, accountNumber);
        return dataStore.getTransactionsForAccount(account.getAccountNumber());
    }
}
