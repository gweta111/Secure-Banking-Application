package com.securebank;

import com.securebank.model.Account;
import com.securebank.model.BankingException;
import com.securebank.model.CheckingAccount;
import com.securebank.model.SavingsAccount;
import com.securebank.model.Transaction;
import com.securebank.model.TransactionType;
import com.securebank.model.User;
import com.securebank.security.InputValidator;
import com.securebank.security.PasswordSecurity;
import com.securebank.security.SecurityAuditLogger;
import com.securebank.service.AuthenticationService;
import com.securebank.service.BankService;
import com.securebank.storage.DataStore;
import com.securebank.storage.FileDataStore;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Comprehensive automated test suite verifying all security controls,
 * OOP principles, business logic, and file persistence.
 */
public class SecurityAndBankingTest {
    private static int totalTests = 0;
    private static int passedTests = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println(" RUNNING SECURE BANKING APPLICATION TEST SUITE                ");
        System.out.println(" Candidate: Robert Kadyamusuma (H250298W)                     ");
        System.out.println("===============================================================\n");

        testPasswordSecurity();
        testInputValidation();
        testAuthenticationAndLockout();
        testAccountPolymorphismAndTransactions();
        testUnauthorizedAccessControl();
        testDataPersistenceReload();

        System.out.println("\n===============================================================");
        System.out.printf(" TEST RESULTS: %d / %d PASSED (%.1f%%)\n",
                passedTests, totalTests, ((double) passedTests / totalTests) * 100);
        System.out.println("===============================================================");

        if (passedTests != totalTests) {
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println(" [PASS] " + testName);
        } else {
            System.err.println(" [FAIL] " + testName);
        }
    }

    private static void testPasswordSecurity() {
        System.out.println(">> Testing Password Security & Hashing...");

        String salt1 = PasswordSecurity.generateSalt();
        String salt2 = PasswordSecurity.generateSalt();
        assertTrue("Salts should be unique", !salt1.equals(salt2) && salt1.length() == 32);

        String rawPassword = "SecurePassword123!";
        String hash1 = PasswordSecurity.hashPassword(rawPassword, salt1);
        String hash2 = PasswordSecurity.hashPassword(rawPassword, salt1);
        assertTrue("Deterministic hashing with same salt", hash1.equals(hash2));

        assertTrue("Password verification succeeds with correct password",
                PasswordSecurity.verifyPassword(rawPassword, hash1, salt1));
        assertTrue("Password verification fails with incorrect password",
                !PasswordSecurity.verifyPassword("WrongPassword123!", hash1, salt1));

        // Password policy tests
        List<String> weakViolations = PasswordSecurity.validatePasswordStrength("weak");
        assertTrue("Weak password flagged with violations", !weakViolations.isEmpty());

        List<String> strongViolations = PasswordSecurity.validatePasswordStrength("Complex@Pass1");
        assertTrue("Strong password passes all policy checks", strongViolations.isEmpty());
    }

    private static void testInputValidation() {
        System.out.println("\n>> Testing Input Validation & Sanitization...");

        assertTrue("Valid username passes", InputValidator.isValidUsername("robert_kadya"));
        assertTrue("Username with spaces rejected", !InputValidator.isValidUsername("robert user"));
        assertTrue("Username too short rejected", !InputValidator.isValidUsername("ab"));
        assertTrue("Username with malicious chars rejected", !InputValidator.isValidUsername("user<script>"));

        assertTrue("Valid currency parses correctly", InputValidator.isValidCurrencyString("150.75"));
        assertTrue("Negative currency rejected", !InputValidator.isValidCurrencyString("-50.00"));
        assertTrue("Zero currency rejected", !InputValidator.isValidCurrencyString("0.00"));
        assertTrue("More than 2 decimals rejected", !InputValidator.isValidCurrencyString("100.999"));

        String maliciousInput = "test|account\nDROP|TABLE\r";
        String sanitized = InputValidator.sanitizeForStorage(maliciousInput);
        assertTrue("Sanitizer neutralizes delimiter and newline characters",
                !sanitized.contains("|") && !sanitized.contains("\n") && !sanitized.contains("\r"));
    }

    private static void testAuthenticationAndLockout() {
        System.out.println("\n>> Testing Authentication, Registration, and Brute-force Lockout...");

        try {
            Path tempDir = Files.createTempDirectory("bank_auth_test");
            FileDataStore dataStore = new FileDataStore(
                    tempDir.resolve("users.txt"),
                    tempDir.resolve("accounts.txt"),
                    tempDir.resolve("transactions.txt"));
            SecurityAuditLogger audit = new SecurityAuditLogger(tempDir.resolve("audit.log"));
            AuthenticationService auth = new AuthenticationService(dataStore, audit);

            // 1. Registration
            User user = auth.register("alice_secure", "CorrectHorseBattery99#");
            assertTrue("User registration creates valid user entity", user != null && user.getUsername().equals("alice_secure"));

            // 2. Duplicate registration rejected
            boolean duplicateBlocked = false;
            try {
                auth.register("alice_secure", "DifferentPass123!");
            } catch (BankingException e) {
                duplicateBlocked = true;
            }
            assertTrue("Duplicate username registration is blocked", duplicateBlocked);

            // 3. Successful login
            User loggedIn = auth.login("alice_secure", "CorrectHorseBattery99#");
            assertTrue("Login succeeds with correct password", loggedIn != null);
            assertTrue("Session user is set", auth.getCurrentSessionUser() != null);
            auth.logout();
            assertTrue("Logout clears session", !auth.isAuthenticated());

            // 4. Failed attempts and Lockout
            for (int i = 0; i < 2; i++) {
                try {
                    auth.login("alice_secure", "WrongPassword123!");
                } catch (BankingException ignored) {}
            }
            User userBeforeLock = dataStore.findUserByUsername("alice_secure").orElseThrow();
            assertTrue("Failed attempt count tracks correctly (2)", userBeforeLock.getFailedLoginAttempts() == 2);
            assertTrue("User not yet locked at 2 attempts", !userBeforeLock.isLocked());

            // 3rd attempt triggers lock
            boolean lockedMessageReceived = false;
            try {
                auth.login("alice_secure", "WrongPassword123!");
            } catch (BankingException e) {
                lockedMessageReceived = e.getMessage().contains("locked");
            }
            assertTrue("Lockout triggered after 3 consecutive failures", lockedMessageReceived);

            User lockedUser = dataStore.findUserByUsername("alice_secure").orElseThrow();
            assertTrue("User entity is marked as locked", lockedUser.isLocked());

            // Attempt login even with correct password while locked
            boolean lockedEvenWithCorrect = false;
            try {
                auth.login("alice_secure", "CorrectHorseBattery99#");
            } catch (BankingException e) {
                lockedEvenWithCorrect = e.getMessage().contains("locked");
            }
            assertTrue("Locked account denies login even with valid password", lockedEvenWithCorrect);

        } catch (Exception e) {
            e.printStackTrace();
            assertTrue("Authentication test encountered unexpected error: " + e.getMessage(), false);
        }
    }

    private static void testAccountPolymorphismAndTransactions() {
        System.out.println("\n>> Testing Account Polymorphism (Savings vs Checking) & Transactions...");

        try {
            Path tempDir = Files.createTempDirectory("bank_ops_test");
            FileDataStore dataStore = new FileDataStore(
                    tempDir.resolve("users.txt"),
                    tempDir.resolve("accounts.txt"),
                    tempDir.resolve("transactions.txt"));
            SecurityAuditLogger audit = new SecurityAuditLogger(tempDir.resolve("audit.log"));
            AuthenticationService auth = new AuthenticationService(dataStore, audit);
            BankService bank = new BankService(dataStore, audit);

            User bob = auth.register("bob_builder", "HammerTime2026!");

            // 1. Create Savings Account with $100 (Min balance $50)
            Account savings = bank.createAccount(bob, "SAVINGS", new BigDecimal("100.00"));
            assertTrue("Savings account created with initial deposit", savings instanceof SavingsAccount);
            assertTrue("Savings account balance is $100.00", savings.getBalance().compareTo(new BigDecimal("100.00")) == 0);

            // 2. Deposit into Savings
            bank.deposit(bob, savings.getAccountNumber(), new BigDecimal("50.00"));
            assertTrue("Savings deposit updates balance to $150.00", savings.getBalance().compareTo(new BigDecimal("150.00")) == 0);

            // 3. Polymorphic withdrawal - allowed amount
            bank.withdraw(bob, savings.getAccountNumber(), new BigDecimal("80.00"));
            assertTrue("Balance after $80 withdrawal is $70.00", savings.getBalance().compareTo(new BigDecimal("70.00")) == 0);

            // 4. Polymorphic withdrawal - violating $50 minimum balance
            boolean minBalanceBlocked = false;
            try {
                bank.withdraw(bob, savings.getAccountNumber(), new BigDecimal("30.00")); // would leave $40, below $50 min
            } catch (BankingException e) {
                minBalanceBlocked = true;
            }
            assertTrue("Savings account blocks withdrawal violating minimum balance", minBalanceBlocked);
            assertTrue("Balance unchanged after blocked withdrawal", savings.getBalance().compareTo(new BigDecimal("70.00")) == 0);

            // 5. Create Checking Account with $50 (Overdraft $200)
            Account checking = bank.createAccount(bob, "CHECKING", new BigDecimal("50.00"));
            assertTrue("Checking account created", checking instanceof CheckingAccount);

            // 6. Withdraw into overdraft limit (Withdraw $150 from $50 balance -> -$100, which is >= -$200 limit)
            bank.withdraw(bob, checking.getAccountNumber(), new BigDecimal("150.00"));
            assertTrue("Checking account overdraft allowed (balance is -$100.00)",
                    checking.getBalance().compareTo(new BigDecimal("-100.00")) == 0);

            // 7. Withdraw exceeding overdraft limit (attempt to withdraw another $150 -> -$250, exceeds -$200 limit)
            boolean overdraftBlocked = false;
            try {
                bank.withdraw(bob, checking.getAccountNumber(), new BigDecimal("150.00"));
            } catch (BankingException e) {
                overdraftBlocked = true;
            }
            assertTrue("Checking account blocks withdrawal exceeding overdraft limit", overdraftBlocked);

            // 8. Atomic transfer between accounts
            bank.transfer(bob, savings.getAccountNumber(), checking.getAccountNumber(), new BigDecimal("20.00"));
            assertTrue("Source account debited after transfer ($70 - $20 = $50)",
                    savings.getBalance().compareTo(new BigDecimal("50.00")) == 0);
            assertTrue("Target account credited after transfer (-$100 + $20 = -$80)",
                    checking.getBalance().compareTo(new BigDecimal("-80.00")) == 0);

            // 9. Verify transaction logs generated
            List<Transaction> savingsTxs = bank.getTransactionHistory(bob, savings.getAccountNumber());
            assertTrue("Savings account has recorded transactions", savingsTxs.size() >= 4);

        } catch (Exception e) {
            e.printStackTrace();
            assertTrue("Account operations test failed: " + e.getMessage(), false);
        }
    }

    private static void testUnauthorizedAccessControl() {
        System.out.println("\n>> Testing Authorization & Ownership Security Checks...");

        try {
            Path tempDir = Files.createTempDirectory("bank_authz_test");
            FileDataStore dataStore = new FileDataStore(
                    tempDir.resolve("users.txt"),
                    tempDir.resolve("accounts.txt"),
                    tempDir.resolve("transactions.txt"));
            SecurityAuditLogger audit = new SecurityAuditLogger(tempDir.resolve("audit.log"));
            AuthenticationService auth = new AuthenticationService(dataStore, audit);
            BankService bank = new BankService(dataStore, audit);

            User user1 = auth.register("user_one", "Password123#A");
            User user2 = auth.register("user_two", "Password123#B");

            Account user1Account = bank.createAccount(user1, "SAVINGS", new BigDecimal("200.00"));

            // User 2 attempts to withdraw from User 1's account
            boolean accessDenied = false;
            try {
                bank.withdraw(user2, user1Account.getAccountNumber(), new BigDecimal("50.00"));
            } catch (BankingException e) {
                accessDenied = e.getMessage().contains("Access denied");
            }
            assertTrue("User cannot withdraw from another user's account", accessDenied);

            // User 2 attempts to view User 1's transaction history
            boolean historyDenied = false;
            try {
                bank.getTransactionHistory(user2, user1Account.getAccountNumber());
            } catch (BankingException e) {
                historyDenied = e.getMessage().contains("Access denied");
            }
            assertTrue("User cannot view another user's transaction statement", historyDenied);

        } catch (Exception e) {
            e.printStackTrace();
            assertTrue("Authorization test failed: " + e.getMessage(), false);
        }
    }

    private static void testDataPersistenceReload() {
        System.out.println("\n>> Testing File Persistence & Restart Reloading...");

        try {
            Path tempDir = Files.createTempDirectory("bank_persist_test");
            Path usersPath = tempDir.resolve("users.txt");
            Path accountsPath = tempDir.resolve("accounts.txt");
            Path txPath = tempDir.resolve("transactions.txt");
            Path auditPath = tempDir.resolve("audit.log");

            SecurityAuditLogger audit1 = new SecurityAuditLogger(auditPath);
            FileDataStore store1 = new FileDataStore(usersPath, accountsPath, txPath);
            AuthenticationService auth1 = new AuthenticationService(store1, audit1);
            BankService bank1 = new BankService(store1, audit1);

            User charlie = auth1.register("charlie_brown", "GoodGrief123!");
            Account charlieSavings = bank1.createAccount(charlie, "SAVINGS", new BigDecimal("350.00"));
            bank1.deposit(charlie, charlieSavings.getAccountNumber(), new BigDecimal("150.00"));
            bank1.withdraw(charlie, charlieSavings.getAccountNumber(), new BigDecimal("50.00"));

            // Force save
            store1.saveAll();

            // Simulate application restart by creating completely new DataStore instances reading from disk
            SecurityAuditLogger audit2 = new SecurityAuditLogger(auditPath);
            FileDataStore store2 = new FileDataStore(usersPath, accountsPath, txPath);
            store2.loadAll();

            // Verify loaded user
            User loadedUser = store2.findUserByUsername("charlie_brown").orElse(null);
            assertTrue("Reloaded user exists", loadedUser != null);
            assertTrue("Reloaded user retains hashed password",
                    PasswordSecurity.verifyPassword("GoodGrief123!", loadedUser.getPasswordHash(), loadedUser.getSalt()));

            // Verify loaded account & balance
            Account loadedAccount = store2.findAccountByNumber(charlieSavings.getAccountNumber()).orElse(null);
            assertTrue("Reloaded account exists", loadedAccount != null);
            assertTrue("Reloaded account is SavingsAccount instance", loadedAccount instanceof SavingsAccount);
            assertTrue("Reloaded balance is exactly $450.00",
                    loadedAccount.getBalance().compareTo(new BigDecimal("450.00")) == 0);

            // Verify loaded transactions
            List<Transaction> reloadedTxs = store2.getTransactionsForAccount(charlieSavings.getAccountNumber());
            assertTrue("Transactions correctly reloaded from disk", reloadedTxs.size() == 3);

        } catch (Exception e) {
            e.printStackTrace();
            assertTrue("Persistence reload test failed: " + e.getMessage(), false);
        }
    }
}
