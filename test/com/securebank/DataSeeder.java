package com.securebank;

import com.securebank.model.Account;
import com.securebank.model.User;
import com.securebank.security.SecurityAuditLogger;
import com.securebank.service.AuthenticationService;
import com.securebank.service.BankService;
import com.securebank.storage.DataStore;
import com.securebank.storage.FileDataStore;

import java.math.BigDecimal;
import java.nio.file.Paths;

/**
 * Utility to seed initial sample data for demonstration, grading, and testing.
 */
public class DataSeeder {
    public static void main(String[] args) {
        System.out.println("Seeding demo data into data/ directory...");
        SecurityAuditLogger audit = new SecurityAuditLogger(Paths.get("data", "audit.log"));
        DataStore store = new FileDataStore(
                Paths.get("data", "users.txt"),
                Paths.get("data", "accounts.txt"),
                Paths.get("data", "transactions.txt")
        );

        AuthenticationService auth = new AuthenticationService(store, audit);
        BankService bank = new BankService(store, audit);

        // Register default demonstration user
        User robert = auth.register("robert_kadya", "SecurePass2026!");
        System.out.println("Created User: " + robert.getUsername() + " (Password: SecurePass2026!)");

        // Open a Savings Account
        Account savings = bank.createAccount(robert, "SAVINGS", new BigDecimal("500.00"));
        System.out.println("Created Savings Account: " + savings.getAccountNumber() + " ($500.00)");

        // Open a Checking Account
        Account checking = bank.createAccount(robert, "CHECKING", new BigDecimal("250.00"));
        System.out.println("Created Checking Account: " + checking.getAccountNumber() + " ($250.00)");

        // Perform sample deposit and withdrawal
        bank.deposit(robert, savings.getAccountNumber(), new BigDecimal("100.00"));
        bank.withdraw(robert, savings.getAccountNumber(), new BigDecimal("50.00"));
        bank.transfer(robert, savings.getAccountNumber(), checking.getAccountNumber(), new BigDecimal("75.00"));

        try {
            store.saveAll();
            System.out.println("Sample data seeded and persisted successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
