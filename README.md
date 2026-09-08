# Secure Banking Application

## Description
A robust, console-based banking application developed in Java that demonstrates core Object-Oriented Programming (OOP) principles and critical secure coding practices. The system provides secure user authentication, polymorphic bank account management, financial transactions, and atomic file-based data persistence without external dependencies.

---

## Student Details
- **Student Name:** Robert Kadyamusuma
- **Registration Number:** H250298W
- **Target Repository Name:** `SecureBankApp-H250298W`
- **Course / Project:** Mini-Project: Secure Banking Application (100 Marks)

---

## Features
- **User Authentication System:**
  - Secure registration with strict password complexity enforcement (min 8 characters, uppercase, lowercase, digit, and special symbol).
  - Cryptographic salted password hashing using **SHA-256** and **SecureRandom** salts.
  - Constant-time hash comparison (`MessageDigest.isEqual`) to defend against timing attacks.
  - Automatic account lockout after **3 consecutive failed login attempts** to protect against brute-force attacks.
  - Session management with safe logout handling.
- **Account Management:**
  - Create new accounts linked to the authenticated user.
  - Support for multiple account types with unique business rules:
    - **Savings Account:** Interest-bearing account enforcing a mandatory minimum balance of \$50.00.
    - **Checking Account:** Everyday transactional account featuring \$200.00 overdraft protection.
  - Real-time balance and account details inquiry.
  - Strict ownership access control preventing unauthorized users from viewing or modifying accounts they do not own.
- **Financial Transactions:**
  - Secure cash deposits with positive value and scale validation.
  - Polymorphic cash withdrawals tailored to specific account rules (checking overdraft vs. savings minimum balance).
  - **Atomic Funds Transfer:** Transfer funds safely between accounts with rollback safeguards and synchronized execution.
  - Detailed, printable transaction statement history per account.
  - Financial calculations handled via `BigDecimal` to eliminate IEEE-754 floating-point rounding inaccuracies.
- **Data Persistence:**
  - Full state persistence across application lifecycles using structured text files (`users.txt`, `accounts.txt`, `transactions.txt`).
  - Atomic file swap writing (`.tmp` write and atomic rename) to guard against corruption during power loss or unexpected process termination.
  - Delimiter and newline sanitization to prevent file-level delimiter injection attacks.
- **Security Audit Logging:**
  - Persistent, tamper-evident audit log (`data/audit.log`) capturing timestamps, event categories, actor identity, and operation details.

---

## Pre-Configured Demonstration Account
For quick testing and grading, the repository comes pre-seeded with sample data:
- **Username:** `robert_kadya`
- **Password:** `SecurePass2026!`
- **Pre-existing Accounts:**
  - Savings Account: `ACC-SAV-1001`
  - Checking Account: `ACC-CHK-1002`

You may also register brand new user accounts directly through the application menu.

---

## Object-Oriented Programming (OOP) Principles

| OOP Principle | Implementation Details |
| :--- | :--- |
| **Encapsulation** | All domain fields in `User`, `Account`, `Transaction`, and service states are strictly `private` or `final`. State mutations occur exclusively via validated domain methods (e.g., `deposit()`, `withdraw()`, `incrementFailedAttempts()`). |
| **Abstraction** | The abstract base class `Account` defines standard account behavior while hiding concrete withdrawal validation. The `DataStore` interface abstracts file storage from high-level business services, adhering to the Dependency Inversion Principle. |
| **Inheritance** | `SavingsAccount` and `CheckingAccount` inherit common properties (account number, balance, timestamps) from `Account`, extending it with account-specific attributes (interest rates, minimum balances, overdraft limits). |
| **Polymorphism** | Dynamic method dispatch is utilized during withdrawal operations. Calling `account.withdraw(amount)` polymorphically executes either `SavingsAccount.withdraw()` or `CheckingAccount.withdraw()`, seamlessly enforcing differing overdraft or minimum balance logic. |

---

## Secure Coding Practices

1. **Cryptographic Password Storage:**
   Passwords are never stored in plaintext. Passwords are salted using a 16-byte cryptographically secure random salt generated via `java.security.SecureRandom` and hashed using `SHA-256`.
2. **Timing-Attack Resistance:**
   Authentication uses `MessageDigest.isEqual()` to perform constant-time byte comparisons, eliminating side-channel timing vulnerabilities during credential verification.
3. **Brute-Force Lockout:**
   Users are granted 3 consecutive failed login attempts before their account is automatically locked. Locked accounts reject subsequent attempts, even with valid credentials, until administratively unlocked.
4. **Delimiter & Command Injection Prevention:**
   All user input is sanitized before writing to flat files. Characters such as `|`, `\r`, and `\n` are sanitized or stripped to prevent delimiter tampering or record forging.
5. **Fail-Safe Exception Handling:**
   A custom `BankingException` shields internal stack traces and database implementation details from being leaked to console users.
6. **Financial Precision:**
   All monetary amounts utilize `java.math.BigDecimal` with explicit scale and rounding (`RoundingMode.HALF_UP`) instead of `float` or `double`, preventing catastrophic precision losses.
7. **Atomic File Persistence:**
   File writing updates data to a temporary file (`.tmp`) first and uses `Files.move(..., StandardCopyOption.ATOMIC_MOVE)` to atomically replace the target file, ensuring zero data loss if the system halts unexpectedly.

---

## Project Structure

```
SecureBankApp-H250298W/
├── src/
│   └── com/
│       └── securebank/
│           ├── Main.java                          # Main entry point & shutdown hook
│           ├── model/
│           │   ├── Account.java                   # Abstract base account (Abstraction & Encapsulation)
│           │   ├── BankingException.java          # Custom domain exception
│           │   ├── CheckingAccount.java           # Overdraft-enabled account (Inheritance & Polymorphism)
│           │   ├── SavingsAccount.java            # Minimum balance account (Inheritance & Polymorphism)
│           │   ├── Transaction.java               # Immutable transaction model
│           │   ├── TransactionType.java           # Transaction type enumeration
│           │   └── User.java                      # User entity with security attributes
│           ├── security/
│           │   ├── InputValidator.java            # Input validation & delimiter sanitization
│           │   ├── PasswordSecurity.java          # Salting, SHA-256 hashing, timing defense
│           │   └── SecurityAuditLogger.java       # Audit logging to data/audit.log
│           ├── service/
│           │   ├── AuthenticationService.java     # Login, registration, lockout logic
│           │   └── BankService.java               # Accounts, deposits, withdrawals, transfers
│           ├── storage/
│           │   ├── DataStore.java                 # Storage abstraction interface
│           │   └── FileDataStore.java             # Thread-safe atomic file persistence
│           └── ui/
│               └── ConsoleInterface.java          # Terminal UI menus and CLI interactions
├── test/
│   └── com/
│       └── securebank/
│           ├── DataSeeder.java                    # Demo data initialization utility
│           └── SecurityAndBankingTest.java        # Automated 45-step unit & security test suite
├── data/
│   ├── users.txt                                  # Persisted user records
│   ├── accounts.txt                               # Persisted account records
│   ├── transactions.txt                           # Persisted transaction statements
│   └── audit.log                                  # Security audit trail
├── compile_and_run.bat                            # Windows build and run script
├── compile_and_run.sh                             # Linux/macOS build and run script
├── SecureBankApp.exe                              # Native Windows binary executable (Double-click to launch)
├── SecureBankApp.bat                              # One-click Windows batch launcher
├── SecureBankApp.jar                              # Standalone executable JAR package
├── build_all.bat                                  # Master build script (compiles code, JAR, and EXE)
├── launcher/                                      # C# native launcher source code
├── .gitignore                                     # Git ignore configuration
└── README.md                                      # Documentation & technical manual
```

---

## Installation and Execution Instructions

### Prerequisites
- Java Development Kit (**JDK 17 or higher**; tested and verified on OpenJDK / Oracle JDK 26).
- Git installed on your system.

---

### Launching on Windows (Instant 1-Click Launch)

You have multiple convenient ways to run the application with a single click:

#### Option 1: Native Windows Binary Executable (Recommended)
Simply **double-click** `SecureBankApp.exe` directly in Windows File Explorer:
- Opens a dedicated, styled terminal window.
- Automatically compiles source code if running for the first time.
- Launches the interactive banking application instantly without touching the command line.

#### Option 2: One-Click Batch Launcher
Double-click `SecureBankApp.bat` or run:
```cmd
SecureBankApp.bat
```

#### Option 3: Executable JAR Package
Run via standard Java:
```cmd
java -jar SecureBankApp.jar
```

#### Option 4: Manual Compilation & Execution
```powershell
# 1. Create output bin directory
mkdir bin

# 2. Compile all source files
javac -d bin -sourcepath src src\com\securebank\Main.java

# 3. Run the application
java -cp bin com.securebank.Main
```

---

### Running on Linux or macOS

#### Option 1: Using the automated shell script
```bash
chmod +x compile_and_run.sh
./compile_and_run.sh
```

#### Option 2: Manual compilation and execution
```bash
# 1. Create output bin directory
mkdir -p bin

# 2. Compile all source files
javac -d bin -sourcepath src src/com/securebank/Main.java

# 3. Run the application
java -cp bin com.securebank.Main
```

---

## Running the Automated Test Suite

A comprehensive automated test runner is included in `test/com/securebank/SecurityAndBankingTest.java`. It executes **45 verification checks** covering:
- Cryptographic salt generation & deterministic SHA-256 hashing
- Constant-time password validation
- Password complexity policy compliance
- Username & currency input validation
- Malicious delimiter sanitization
- User registration, duplicate detection, and session handling
- Brute-force account lockout after 3 consecutive failures
- Savings account minimum balance constraint enforcement
- Checking account overdraft protection limit checks
- Atomic funds transfer integrity between accounts
- Access control preventing unauthorized access to other users' accounts
- Complete persistence reload test verifying disk read/write consistency

### Running tests:
```bash
# Compile tests
javac -cp bin -d bin test/com/securebank/SecurityAndBankingTest.java

# Run test suite
java -cp bin com.securebank.SecurityAndBankingTest
```

**Expected Output:**
```
===============================================================
 RUNNING SECURE BANKING APPLICATION TEST SUITE                
 Candidate: Robert Kadyamusuma (H250298W)                     
===============================================================
>> Testing Password Security & Hashing...
 [PASS] Salts should be unique
 [PASS] Deterministic hashing with same salt
 [PASS] Password verification succeeds with correct password
 ...
===============================================================
 TEST RESULTS: 45 / 45 PASSED (100.0%)
===============================================================
```

---

## Submitting to GitHub

To submit your repository according to the project specifications:

1. Repository on GitHub:
   ```
   https://github.com/gweta111/Secure-Banking-Application.git
   ```
2. Link your local repository and push:
   ```bash
   git remote add origin https://github.com/gweta111/Secure-Banking-Application.git
   git branch -M main
   git push -u origin main
   ```
3. Copy your public GitHub repository link and submit it on the university E-learning platform before the submission deadline.
