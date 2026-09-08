package com.securebank.storage;

import com.securebank.model.Account;
import com.securebank.model.Transaction;
import com.securebank.model.User;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * DataStore interface defining persistence operations.
 * Demonstrates Abstraction and Dependency Inversion.
 */
public interface DataStore {
    void loadAll() throws IOException;
    void saveAll() throws IOException;

    // User persistence
    Optional<User> findUserById(String userId);
    Optional<User> findUserByUsername(String username);
    void saveUser(User user);
    List<User> getAllUsers();

    // Account persistence
    Optional<Account> findAccountByNumber(String accountNumber);
    List<Account> findAccountsByUserId(String userId);
    void saveAccount(Account account);
    List<Account> getAllAccounts();

    // Transaction persistence
    void recordTransaction(Transaction transaction);
    List<Transaction> getTransactionsForAccount(String accountNumber);
    List<Transaction> getAllTransactions();
}
