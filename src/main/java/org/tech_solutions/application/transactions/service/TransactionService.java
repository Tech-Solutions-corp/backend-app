package org.tech_solutions.application.transactions.service;

import org.springframework.stereotype.Service;
import org.tech_solutions.application.accounts.model.Account;
import org.tech_solutions.application.accounts.repository.AccountRepository;
import org.tech_solutions.application.categories.model.Category;
import org.tech_solutions.application.categories.repository.CategoryRepository;
import org.tech_solutions.application.shared.exception.EntityNotFoundException;
import org.tech_solutions.application.transactions.model.Transaction;
import org.tech_solutions.application.transactions.repository.TransactionRepository;
import org.tech_solutions.application.user.model.User;
import org.tech_solutions.application.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    public Transaction create(Transaction transaction, Long userId, Long accountId, Long categoryId) {
        transaction.setUser(findUser(userId));
        transaction.setAccount(findAccount(accountId));
        transaction.setCategory(findCategory(categoryId));
        transaction.setCreatedAt(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    public List<Transaction> listAll() {
        return transactionRepository.findAll();
    }

    public List<Transaction> listByUser(Long userId) {
        findUser(userId);
        return transactionRepository.findByUserId(userId);
    }

    public Transaction findById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transacao nao encontrada"));
    }

    public Transaction update(
            Long id,
            Transaction updated,
            Long userId,
            Long accountId,
            Long categoryId
    ) {
        Transaction current = findById(id);
        current.setUser(findUser(userId));
        current.setAccount(findAccount(accountId));
        current.setCategory(findCategory(categoryId));
        current.setTransactionDescription(updated.getTransactionDescription());
        current.setAmount(updated.getAmount());
        current.setTransactionDate(updated.getTransactionDate());
        current.setTransactionType(updated.getTransactionType());
        return transactionRepository.save(current);
    }

    public void delete(Long id) {
        transactionRepository.delete(findById(id));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));
    }

    private Account findAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Conta nao encontrada"));
    }

    private Category findCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Categoria nao encontrada"));
    }
}


