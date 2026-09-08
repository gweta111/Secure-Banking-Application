package com.securebank.storage;

import com.securebank.model.Account;
import com.securebank.model.BankingException;
import com.securebank.model.CheckingAccount;
import com.securebank.model.SavingsAccount;
import com.securebank.model.Transaction;
import com.securebank.model.TransactionType;
import com.securebank.model.User;
import com.securebank.security.InputValidator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Robust, thread-safe file-based implementation of DataStore.
 * Demonstrates secure persistence to text files using atomic writes and defensive parsing.
 */
public class FileDataStore implements DataStore {
    private static final String DELIMITER = "|";
    private static final String DELIMITER_REGEX = "\\|";

    private final Path usersFile;
    private final Path accountsFile;
    private final Path transactionsFile;

    private final Map<String, User> userMap = new ConcurrentHashMap<>();
    private final Map<String, Account> accountMap = new ConcurrentHashMap<>();
    private final List<Transaction> transactionList = Collections.synchronizedList(new ArrayList<>());

    public FileDataStore() {
        this(Paths.get("data", "users.txt"),
             Paths.get("data", "accounts.txt"),
             Paths.get("data", "transactions.txt"));
    }

    public FileDataStore(Path usersFile, Path accountsFile, Path transactionsFile) {
        this.usersFile = usersFile;
        this.accountsFile = accountsFile;
        this.transactionsFile = transactionsFile;
        initDirectoriesAndFiles();
    }

    private void initDirectoriesAndFiles() {
        try {
            ensureParentAndFile(usersFile);
            ensureParentAndFile(accountsFile);
            ensureParentAndFile(transactionsFile);
        } catch (IOException e) {
            System.err.println("[STORAGE ERROR] Failed to initialize storage files: " + e.getMessage());
        }
    }

    private void ensureParentAndFile(Path path) throws IOException {
        if (path.getParent() != null && !Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
    }

    @Override
    public synchronized void loadAll() throws IOException {
        userMap.clear();
        accountMap.clear();
        transactionList.clear();

        loadUsers();
        loadAccounts();
        loadTransactions();
    }

    private void loadUsers() throws IOException {
        if (!Files.exists(usersFile)) return;
        try (BufferedReader reader = Files.newBufferedReader(usersFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(DELIMITER_REGEX);
                if (parts.length >= 7) {
                    try {
                        String userId = parts[0];
                        String username = parts[1];
                        String passwordHash = parts[2];
                        String salt = parts[3];
                        int failedAttempts = Integer.parseInt(parts[4]);
                        boolean isLocked = Boolean.parseBoolean(parts[5]);
                        LocalDateTime createdAt = LocalDateTime.parse(parts[6]);

                        User user = new User(userId, username, passwordHash, salt, failedAttempts, isLocked, createdAt);
                        userMap.put(user.getUserId(), user);
                    } catch (Exception e) {
                        System.err.println("[PARSE ERROR] Skipping corrupted user record: " + line);
                    }
                }
            }
        }
    }

    private void loadAccounts() throws IOException {
        if (!Files.exists(accountsFile)) return;
        try (BufferedReader reader = Files.newBufferedReader(accountsFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(DELIMITER_REGEX);
                if (parts.length >= 5) {
                    try {
                        String accountNumber = parts[0];
                        String userId = parts[1];
                        String type = parts[2];
                        BigDecimal balance = new BigDecimal(parts[3]);
                        LocalDateTime createdAt = LocalDateTime.parse(parts[4]);

                        Account account;
                        if ("SAVINGS".equalsIgnoreCase(type)) {
                            BigDecimal minBalance = (parts.length > 5 && !parts[5].isEmpty())
                                    ? new BigDecimal(parts[5]) : SavingsAccount.DEFAULT_MIN_BALANCE;
                            BigDecimal interestRate = (parts.length > 6 && !parts[6].isEmpty())
                                    ? new BigDecimal(parts[6]) : SavingsAccount.DEFAULT_INTEREST_RATE;
                            account = new SavingsAccount(accountNumber, userId, balance, createdAt, minBalance, interestRate);
                        } else if ("CHECKING".equalsIgnoreCase(type)) {
                            BigDecimal overdraft = (parts.length > 5 && !parts[5].isEmpty())
                                    ? new BigDecimal(parts[5]) : CheckingAccount.DEFAULT_OVERDRAFT_LIMIT;
                            account = new CheckingAccount(accountNumber, userId, balance, createdAt, overdraft);
                        } else {
                            System.err.println("[PARSE ERROR] Unknown account type: " + type);
                            continue;
                        }

                        accountMap.put(account.getAccountNumber(), account);
                    } catch (Exception e) {
                        System.err.println("[PARSE ERROR] Skipping corrupted account record: " + line + " - " + e.getMessage());
                    }
                }
            }
        }
    }

    private void loadTransactions() throws IOException {
        if (!Files.exists(transactionsFile)) return;
        try (BufferedReader reader = Files.newBufferedReader(transactionsFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(DELIMITER_REGEX);
                if (parts.length >= 7) {
                    try {
                        String txId = parts[0];
                        String accNum = parts[1];
                        TransactionType type = TransactionType.valueOf(parts[2]);
                        BigDecimal amount = new BigDecimal(parts[3]);
                        LocalDateTime timestamp = LocalDateTime.parse(parts[4]);
                        BigDecimal resultingBalance = new BigDecimal(parts[5]);
                        String description = parts[6];

                        Transaction tx = new Transaction(txId, accNum, type, amount, timestamp, resultingBalance, description);
                        transactionList.add(tx);
                    } catch (Exception e) {
                        System.err.println("[PARSE ERROR] Skipping corrupted transaction record: " + line);
                    }
                }
            }
        }
    }

    @Override
    public synchronized void saveAll() throws IOException {
        saveUsers();
        saveAccounts();
        saveTransactions();
    }

    private void atomicWriteToFile(Path targetFile, List<String> lines) throws IOException {
        Path tempFile = targetFile.resolveSibling(targetFile.getFileName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }

        try {
            Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void saveUsers() throws IOException {
        List<String> lines = new ArrayList<>();
        for (User user : userMap.values()) {
            lines.add(String.join(DELIMITER,
                    user.getUserId(),
                    InputValidator.sanitizeForStorage(user.getUsername()),
                    user.getPasswordHash(),
                    user.getSalt(),
                    String.valueOf(user.getFailedLoginAttempts()),
                    String.valueOf(user.isLocked()),
                    user.getCreatedAt().toString()
            ));
        }
        atomicWriteToFile(usersFile, lines);
    }

    private void saveAccounts() throws IOException {
        List<String> lines = new ArrayList<>();
        for (Account account : accountMap.values()) {
            String extra1 = "";
            String extra2 = "";
            if (account instanceof SavingsAccount sa) {
                extra1 = sa.getMinimumBalance().toPlainString();
                extra2 = sa.getInterestRate().toPlainString();
            } else if (account instanceof CheckingAccount ca) {
                extra1 = ca.getOverdraftLimit().toPlainString();
            }

            lines.add(String.join(DELIMITER,
                    account.getAccountNumber(),
                    account.getUserId(),
                    account.getAccountType(),
                    account.getBalance().toPlainString(),
                    account.getCreatedAt().toString(),
                    extra1,
                    extra2
            ));
        }
        atomicWriteToFile(accountsFile, lines);
    }

    private void saveTransactions() throws IOException {
        List<String> lines = new ArrayList<>();
        synchronized (transactionList) {
            for (Transaction tx : transactionList) {
                lines.add(String.join(DELIMITER,
                        tx.getTransactionId(),
                        tx.getAccountNumber(),
                        tx.getType().name(),
                        tx.getAmount().toPlainString(),
                        tx.getTimestamp().toString(),
                        tx.getResultingBalance().toPlainString(),
                        InputValidator.sanitizeForStorage(tx.getDescription())
                ));
            }
        }
        atomicWriteToFile(transactionsFile, lines);
    }

    @Override
    public Optional<User> findUserById(String userId) {
        return Optional.ofNullable(userMap.get(userId));
    }

    @Override
    public Optional<User> findUserByUsername(String username) {
        if (username == null) return Optional.empty();
        return userMap.values().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username.trim()))
                .findFirst();
    }

    @Override
    public synchronized void saveUser(User user) {
        userMap.put(user.getUserId(), user);
        try {
            saveUsers();
        } catch (IOException e) {
            throw new BankingException("Failed to persist user updates: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> getAllUsers() {
        return new ArrayList<>(userMap.values());
    }

    @Override
    public Optional<Account> findAccountByNumber(String accountNumber) {
        if (accountNumber == null) return Optional.empty();
        return Optional.ofNullable(accountMap.get(accountNumber.trim().toUpperCase()));
    }

    @Override
    public List<Account> findAccountsByUserId(String userId) {
        if (userId == null) return Collections.emptyList();
        return accountMap.values().stream()
                .filter(a -> a.getUserId().equalsIgnoreCase(userId))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized void saveAccount(Account account) {
        accountMap.put(account.getAccountNumber(), account);
        try {
            saveAccounts();
        } catch (IOException e) {
            throw new BankingException("Failed to persist account updates: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Account> getAllAccounts() {
        return new ArrayList<>(accountMap.values());
    }

    @Override
    public synchronized void recordTransaction(Transaction transaction) {
        transactionList.add(transaction);
        try {
            saveTransactions();
        } catch (IOException e) {
            throw new BankingException("Failed to persist transaction log: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> getTransactionsForAccount(String accountNumber) {
        if (accountNumber == null) return Collections.emptyList();
        synchronized (transactionList) {
            return transactionList.stream()
                    .filter(t -> t.getAccountNumber().equalsIgnoreCase(accountNumber.trim()))
                    .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp())) // Newest first
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<Transaction> getAllTransactions() {
        synchronized (transactionList) {
            return new ArrayList<>(transactionList);
        }
    }
}
