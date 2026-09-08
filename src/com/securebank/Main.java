package com.securebank;

import com.securebank.security.SecurityAuditLogger;
import com.securebank.service.AuthenticationService;
import com.securebank.service.BankService;
import com.securebank.storage.DataStore;
import com.securebank.storage.FileDataStore;
import com.securebank.ui.ConsoleInterface;

import java.io.IOException;

/**
 * Main application entry point for the Secure Banking Application.
 *
 * Student Details:
 *   Name       : Robert Kadyamusuma
 *   Reg Number : H250298W
 *   Project    : SecureBankApp-H250298W
 */
public class Main {
    public static void main(String[] args) {
        // Initialize Security Audit Logger
        SecurityAuditLogger auditLogger = new SecurityAuditLogger();
        auditLogger.log("SYSTEM_STARTUP", "SYSTEM", "Secure Banking Application initializing...");

        // Initialize DataStore and load persisted data
        DataStore dataStore = new FileDataStore();
        try {
            dataStore.loadAll();
            auditLogger.log("DATA_LOADED", "SYSTEM", "Existing users, accounts, and transactions loaded successfully.");
        } catch (IOException e) {
            System.err.println("[WARNING] Could not load persisted data files: " + e.getMessage());
            auditLogger.log("DATA_LOAD_ERROR", "SYSTEM", "Error loading data files: " + e.getMessage());
        }

        // Initialize Service layer
        AuthenticationService authService = new AuthenticationService(dataStore, auditLogger);
        BankService bankService = new BankService(dataStore, auditLogger);

        // Register runtime shutdown hook to safely persist all state upon program termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                dataStore.saveAll();
                auditLogger.log("SYSTEM_SHUTDOWN", "SYSTEM", "Secure Banking Application terminated cleanly.");
            } catch (IOException e) {
                System.err.println("[ERROR] Failed to flush data during shutdown: " + e.getMessage());
            }
        }));

        // Launch Console User Interface
        ConsoleInterface ui = new ConsoleInterface(authService, bankService);
        ui.start();
    }
}
