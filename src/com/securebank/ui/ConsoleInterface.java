package com.securebank.ui;

import com.securebank.model.Account;
import com.securebank.model.BankingException;
import com.securebank.model.Transaction;
import com.securebank.model.User;
import com.securebank.security.InputValidator;
import com.securebank.security.PasswordSecurity;
import com.securebank.service.AuthenticationService;
import com.securebank.service.BankService;

import java.io.Console;
import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

/**
 * Interactive console interface for the Secure Banking Application.
 * Presents clear menus, validates user input, handles exceptions gracefully,
 * and maintains clean separation from business logic.
 */
public class ConsoleInterface {
    private final AuthenticationService authService;
    private final BankService bankService;
    private final Scanner scanner;

    public ConsoleInterface(AuthenticationService authService, BankService bankService) {
        this.authService = authService;
        this.bankService = bankService;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        printBanner();
        boolean running = true;
        while (running) {
            if (!authService.isAuthenticated()) {
                running = showPreLoginMenu();
            } else {
                showPostLoginMenu();
            }
        }
        System.out.println("\nThank you for choosing Secure Bank App. Goodbye!");
    }

    private void printBanner() {
        System.out.println("===============================================================");
        System.out.println("                   SECURE BANKING APPLICATION                 ");
        System.out.println("       Demonstrating Robust OOP Principles & Secure Coding    ");
        System.out.println("       Developer : Robert Kadyamusuma                         ");
        System.out.println("       Reg Number: H250298W                                   ");
        System.out.println("===============================================================");
    }

    private boolean showPreLoginMenu() {
        System.out.println("\n--- MAIN AUTHENTICATION MENU ---");
        System.out.println("1. Login to Existing Account");
        System.out.println("2. Register New User");
        System.out.println("3. Test / Validate Password Policy");
        System.out.println("4. Exit Application");
        System.out.print("Select an option (1-4): ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1" -> handleLogin();
            case "2" -> handleRegistration();
            case "3" -> handleTestPasswordPolicy();
            case "4" -> {
                return false;
            }
            default -> System.out.println("[!] Invalid option. Please enter 1, 2, 3, or 4.");
        }
        return true;
    }

    private void handleLogin() {
        System.out.println("\n--- USER LOGIN ---");
        System.out.print("Enter Username: ");
        String username = scanner.nextLine().trim();
        String password = readPassword("Enter Password: ");

        try {
            User user = authService.login(username, password);
            System.out.println("\n[+] Authentication Successful! Welcome, " + user.getUsername() + ".");
        } catch (BankingException e) {
            System.out.println("[-] Authentication Error: " + e.getMessage());
        }
    }

    private void handleRegistration() {
        System.out.println("\n--- USER REGISTRATION ---");
        System.out.println("Password Policy Requirements:");
        System.out.println(" - At least 8 characters");
        System.out.println(" - At least one uppercase letter (A-Z)");
        System.out.println(" - At least one lowercase letter (a-z)");
        System.out.println(" - At least one numeric digit (0-9)");
        System.out.println(" - At least one special symbol (!@#$%^&*...)");
        System.out.println("----------------------------------------------");

        System.out.print("Enter Desired Username (3-20 characters): ");
        String username = scanner.nextLine().trim();

        String password = readPassword("Enter Strong Password: ");
        displayPasswordStrengthEvaluation(password);

        String confirmPassword = readPassword("Confirm Password: ");

        if (!password.equals(confirmPassword)) {
            System.out.println("[-] Registration Error: Passwords do not match.");
            return;
        }

        try {
            User newUser = authService.register(username, password);
            System.out.println("\n[+] Registration Successful! User ID: " + newUser.getUserId());
            System.out.println("[+] You may now log in with your credentials.");
        } catch (BankingException e) {
            System.out.println("[-] Registration Error: " + e.getMessage());
        }
    }

    private void showPostLoginMenu() {
        User currentUser = authService.getCurrentSessionUser();
        System.out.println("\n===============================================================");
        System.out.println(" DASHBOARD | Active User: " + currentUser.getUsername() + " (" + currentUser.getUserId() + ")");
        System.out.println("===============================================================");
        System.out.println("1. View Accounts & Balances");
        System.out.println("2. Open New Bank Account");
        System.out.println("3. Deposit Funds");
        System.out.println("4. Withdraw Funds");
        System.out.println("5. Transfer Funds to Another Account");
        System.out.println("6. View Account Transaction History");
        System.out.println("7. Verify Account Password & Security Status");
        System.out.println("8. Logout");
        System.out.print("Select an option (1-8): ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1" -> handleViewAccounts(currentUser);
            case "2" -> handleOpenAccount(currentUser);
            case "3" -> handleDeposit(currentUser);
            case "4" -> handleWithdraw(currentUser);
            case "5" -> handleTransfer(currentUser);
            case "6" -> handleTransactionHistory(currentUser);
            case "7" -> handleVerifyPassword(currentUser);
            case "8" -> {
                authService.logout();
                System.out.println("[+] You have been logged out successfully.");
            }
            default -> System.out.println("[!] Invalid option. Please select 1 through 8.");
        }
    }

    private void handleViewAccounts(User user) {
        System.out.println("\n--- YOUR ACCOUNTS ---");
        List<Account> accounts = bankService.getUserAccounts(user);
        if (accounts.isEmpty()) {
            System.out.println("You do not currently have any active bank accounts.");
            System.out.println("Select option 2 from the dashboard to open a Savings or Checking account.");
            return;
        }

        System.out.println(String.format("%-16s | %-10s | %-12s | %s", "Account Number", "Type", "Balance", "Account Details"));
        System.out.println("-----------------------------------------------------------------------------------");
        for (Account acc : accounts) {
            System.out.println(String.format("%-16s | %-10s | $%10.2f  | %s",
                    acc.getAccountNumber(),
                    acc.getAccountType(),
                    acc.getBalance(),
                    acc.getAccountSummary()));
        }
    }

    private void handleOpenAccount(User user) {
        System.out.println("\n--- OPEN NEW BANK ACCOUNT ---");
        System.out.println("Account Types:");
        System.out.println("  1. SAVINGS  (Earns interest, requires minimum balance of $50.00)");
        System.out.println("  2. CHECKING (Day-to-day transactions, $200.00 overdraft protection)");
        System.out.print("Select Account Type (1 for Savings, 2 for Checking): ");

        String typeChoice = scanner.nextLine().trim();
        String type;
        if (typeChoice.equals("1") || typeChoice.equalsIgnoreCase("SAVINGS")) {
            type = "SAVINGS";
        } else if (typeChoice.equals("2") || typeChoice.equalsIgnoreCase("CHECKING")) {
            type = "CHECKING";
        } else {
            System.out.println("[-] Invalid selection. Account creation aborted.");
            return;
        }

        System.out.print("Enter Initial Deposit Amount ($): ");
        String amountStr = scanner.nextLine().trim();

        try {
            BigDecimal initialDeposit = InputValidator.parseAmount(amountStr);
            Account newAcc = bankService.createAccount(user, type, initialDeposit);
            System.out.println("\n[+] Account successfully created!");
            System.out.println("    Account Number : " + newAcc.getAccountNumber());
            System.out.println("    Account Type   : " + newAcc.getAccountType());
            System.out.println("    Opening Balance: $" + newAcc.getBalance());
        } catch (IllegalArgumentException | BankingException e) {
            System.out.println("[-] Account Creation Error: " + e.getMessage());
        }
    }

    private void handleDeposit(User user) {
        System.out.println("\n--- DEPOSIT FUNDS ---");
        List<Account> accounts = bankService.getUserAccounts(user);
        if (accounts.isEmpty()) {
            System.out.println("[-] No active accounts found. Please open an account first.");
            return;
        }

        System.out.print("Enter Account Number (e.g. " + accounts.get(0).getAccountNumber() + "): ");
        String accNum = scanner.nextLine().trim();

        System.out.print("Enter Deposit Amount ($): ");
        String amountStr = scanner.nextLine().trim();

        try {
            BigDecimal amount = InputValidator.parseAmount(amountStr);
            Transaction tx = bankService.deposit(user, accNum, amount);
            System.out.println("\n[+] Deposit Successful!");
            System.out.println("    Transaction ID  : " + tx.getTransactionId());
            System.out.println("    Deposited Amount: $" + tx.getAmount());
            System.out.println("    Updated Balance : $" + tx.getResultingBalance());
        } catch (IllegalArgumentException | BankingException e) {
            System.out.println("[-] Deposit Error: " + e.getMessage());
        }
    }

    private void handleWithdraw(User user) {
        System.out.println("\n--- WITHDRAW FUNDS ---");
        List<Account> accounts = bankService.getUserAccounts(user);
        if (accounts.isEmpty()) {
            System.out.println("[-] No active accounts found. Please open an account first.");
            return;
        }

        System.out.print("Enter Account Number (e.g. " + accounts.get(0).getAccountNumber() + "): ");
        String accNum = scanner.nextLine().trim();

        System.out.print("Enter Withdrawal Amount ($): ");
        String amountStr = scanner.nextLine().trim();

        try {
            BigDecimal amount = InputValidator.parseAmount(amountStr);
            Transaction tx = bankService.withdraw(user, accNum, amount);
            System.out.println("\n[+] Withdrawal Successful!");
            System.out.println("    Transaction ID  : " + tx.getTransactionId());
            System.out.println("    Withdrawn Amount: $" + tx.getAmount());
            System.out.println("    Remaining Balance: $" + tx.getResultingBalance());
        } catch (IllegalArgumentException | BankingException e) {
            System.out.println("[-] Withdrawal Error: " + e.getMessage());
        }
    }

    private void handleTransfer(User user) {
        System.out.println("\n--- TRANSFER FUNDS ---");
        List<Account> accounts = bankService.getUserAccounts(user);
        if (accounts.isEmpty()) {
            System.out.println("[-] No active accounts found. Please open an account first.");
            return;
        }

        System.out.print("Enter Source Account Number (Your account): ");
        String fromAcc = scanner.nextLine().trim();

        System.out.print("Enter Destination Account Number: ");
        String toAcc = scanner.nextLine().trim();

        System.out.print("Enter Transfer Amount ($): ");
        String amountStr = scanner.nextLine().trim();

        try {
            BigDecimal amount = InputValidator.parseAmount(amountStr);
            bankService.transfer(user, fromAcc, toAcc, amount);
            Account updatedSource = bankService.getVerifiedAccount(user, fromAcc);
            System.out.println("\n[+] Transfer Completed Successfully!");
            System.out.println("    Transferred: $" + amount);
            System.out.println("    From Account: " + fromAcc);
            System.out.println("    To Account  : " + toAcc);
            System.out.println("    New Balance for " + fromAcc + ": $" + updatedSource.getBalance());
        } catch (IllegalArgumentException | BankingException e) {
            System.out.println("[-] Transfer Error: " + e.getMessage());
        }
    }

    private void handleTransactionHistory(User user) {
        System.out.println("\n--- TRANSACTION HISTORY ---");
        List<Account> accounts = bankService.getUserAccounts(user);
        if (accounts.isEmpty()) {
            System.out.println("[-] No active accounts found.");
            return;
        }

        System.out.print("Enter Account Number to view statement: ");
        String accNum = scanner.nextLine().trim();

        try {
            List<Transaction> transactions = bankService.getTransactionHistory(user, accNum);
            Account account = bankService.getVerifiedAccount(user, accNum);

            System.out.println("\nStatement for Account: " + accNum + " (" + account.getAccountType() + ")");
            System.out.println("Current Balance: $" + account.getBalance());
            System.out.println("-----------------------------------------------------------------------------------------------------");
            if (transactions.isEmpty()) {
                System.out.println("No recorded transactions found for this account.");
            } else {
                for (Transaction tx : transactions) {
                    System.out.println(tx);
                }
            }
            System.out.println("-----------------------------------------------------------------------------------------------------");
        } catch (BankingException e) {
            System.out.println("[-] Statement Error: " + e.getMessage());
        }
    }

    private void handleVerifyPassword(User user) {
        System.out.println("\n--- VERIFY ACCOUNT PASSWORD ---");
        System.out.println("Active User: " + user.getUsername());
        System.out.println("You can test whether a password is correct for your active account.");
        String testPassword = readPassword("Enter Password to Verify: ");

        boolean isMatch = PasswordSecurity.verifyPassword(testPassword, user.getPasswordHash(), user.getSalt());
        if (isMatch) {
            System.out.println("\n[+] SUCCESS: The password entered is CORRECT!");
            System.out.println("    - Verification: Cryptographic SHA-256 with 16-byte salt matched.");
            System.out.println("    - Timing Protection: Verified via MessageDigest.isEqual constant-time comparison.");
            System.out.println("    - Lockout Status: " + (user.isLocked() ? "LOCKED" : "ACTIVE (Not Locked)"));
            System.out.println("    - Failed Login Attempts: " + user.getFailedLoginAttempts());
            displayPasswordStrengthEvaluation(testPassword);
        } else {
            System.out.println("\n[-] FAILED: The password entered is INCORRECT.");
            System.out.println("    The candidate password does not match the stored credentials for '" + user.getUsername() + "'.");
        }
    }

    private void handleTestPasswordPolicy() {
        System.out.println("\n--- PASSWORD COMPLEXITY & POLICY VALIDATOR ---");
        System.out.println("Test any candidate password to check if it satisfies bank security standards.");
        String testPassword = readPassword("Enter Candidate Password to Test: ");
        displayPasswordStrengthEvaluation(testPassword);
    }

    private void displayPasswordStrengthEvaluation(String password) {
        System.out.println("\nPassword Policy Evaluation:");
        boolean len = password != null && password.length() >= 8;
        boolean upper = password != null && password.chars().anyMatch(Character::isUpperCase);
        boolean lower = password != null && password.chars().anyMatch(Character::isLowerCase);
        boolean digit = password != null && password.chars().anyMatch(Character::isDigit);
        boolean special = password != null && password.chars().anyMatch(ch -> "!@#$%^&*()-_=+[]{}|;:,.<>?/".indexOf(ch) >= 0);

        System.out.printf("  [%s] Minimum 8 characters (Length: %d)\n", len ? "PASS" : "FAIL", password != null ? password.length() : 0);
        System.out.printf("  [%s] At least one uppercase letter (A-Z)\n", upper ? "PASS" : "FAIL");
        System.out.printf("  [%s] At least one lowercase letter (a-z)\n", lower ? "PASS" : "FAIL");
        System.out.printf("  [%s] At least one numeric digit (0-9)\n", digit ? "PASS" : "FAIL");
        System.out.printf("  [%s] At least one special symbol (!@#$...)\n", special ? "PASS" : "FAIL");

        List<String> violations = PasswordSecurity.validatePasswordStrength(password);
        if (violations.isEmpty()) {
            System.out.println("[+] Result: Password meets all security policy requirements.\n");
        } else {
            System.out.println("[-] Result: Password does not meet policy requirements.\n");
        }
    }

    /**
     * Reads a password securely from standard input.
     * When running interactively, supports viewing/confirming the password.
     * Falls back to Scanner for automated scripts or environments without a system console.
     */
    private String readPassword(String prompt) {
        Console console = System.console();
        String password = "";

        if (console != null) {
            char[] chars = console.readPassword(prompt);
            password = (chars != null) ? new String(chars) : "";

            if (!password.isEmpty()) {
                System.out.print("  [?] View entered password to verify? (y/N): ");
                String viewChoice = scanner.nextLine().trim();
                if (viewChoice.equalsIgnoreCase("y") || viewChoice.equalsIgnoreCase("yes")) {
                    System.out.println("  --> Entered Password: [" + password + "]");
                    System.out.print("  Is this correct? (Y/n): ");
                    String confirm = scanner.nextLine().trim();
                    if (confirm.equalsIgnoreCase("n") || confirm.equalsIgnoreCase("no")) {
                        System.out.println("  [!] Please re-enter your password.");
                        return readPassword(prompt);
                    }
                }
            }
        } else {
            System.out.print(prompt);
            password = scanner.nextLine().trim();
        }

        return password;
    }
}
