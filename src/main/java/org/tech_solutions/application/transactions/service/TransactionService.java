package org.tech_solutions.application.transactions.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.tech_solutions.application.accounts.model.Account;
import org.tech_solutions.application.accounts.repository.AccountRepository;
import org.tech_solutions.application.categories.model.Category;
import org.tech_solutions.application.categories.repository.CategoryRepository;
import org.tech_solutions.application.security.CurrentUserService;
import org.tech_solutions.application.shared.exception.EntityNotFoundException;
import org.tech_solutions.application.transactions.enums.TransactionType;
import org.tech_solutions.application.transactions.model.Transaction;
import org.tech_solutions.application.transactions.repository.TransactionRepository;
import org.tech_solutions.application.user.model.User;
import org.tech_solutions.application.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    public TransactionService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            CurrentUserService currentUserService
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
    }

    public Transaction create(Transaction transaction, Long userId, Long accountId, Long categoryId) {
        User currentUser = currentUserService.requireCurrentUser();
        Account account = findOwnedAccount(accountId);
        Category category = findOwnedCategory(categoryId);

        transaction.setUser(currentUser);
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setCreatedAt(LocalDateTime.now());

        applyBalanceChange(account, transaction.getTransactionType(), transaction.getAmount(), false);
        accountRepository.save(account);

        return transactionRepository.save(transaction);
    }

    public List<Transaction> listAll() {
        return transactionRepository.findByUserId(currentUserService.requireCurrentUserId());
    }

    public List<Transaction> listByUser(Long userId) {
        return transactionRepository.findByUserId(currentUserService.requireCurrentUserId());
    }

    public Transaction findById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transacao nao encontrada"));
        assertOwnedByCurrentUser(transaction);
        return transaction;
    }

    public Transaction update(
            Long id,
            Transaction updated,
            Long userId,
            Long accountId,
            Long categoryId
    ) {
        Transaction current = findById(id);
        Account previousAccount = current.getAccount();
        User currentUser = currentUserService.requireCurrentUser();
        Account newAccount = findOwnedAccount(accountId);
        Category newCategory = findOwnedCategory(categoryId);

        boolean sameAccount = previousAccount.getId().equals(newAccount.getId());
        Account accountToAdjust = sameAccount ? previousAccount : newAccount;

        applyBalanceChange(previousAccount, current.getTransactionType(), current.getAmount(), true);

        current.setUser(currentUser);
        current.setAccount(accountToAdjust);
        current.setCategory(newCategory);
        current.setTransactionDescription(updated.getTransactionDescription());
        current.setAmount(updated.getAmount());
        current.setTransactionDate(updated.getTransactionDate());
        current.setTransactionType(updated.getTransactionType());

        applyBalanceChange(accountToAdjust, current.getTransactionType(), current.getAmount(), false);

        accountRepository.save(previousAccount);
        if (!sameAccount) {
            accountRepository.save(newAccount);
        }
        return transactionRepository.save(current);
    }

    public void delete(Long id) {
        Transaction current = findById(id);
        applyBalanceChange(current.getAccount(), current.getTransactionType(), current.getAmount(), true);
        accountRepository.save(current.getAccount());
        transactionRepository.delete(current);
    }

    public void createInitialBalance(Account account, BigDecimal balance) {
        if (balance == null || balance.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        User currentUser = currentUserService.requireCurrentUser();
        Transaction transaction = new Transaction();
        transaction.setUser(currentUser);
        transaction.setAccount(account);
        transaction.setTransactionDescription("Saldo Inicial");
        transaction.setAmount(balance);
        transaction.setTransactionType(TransactionType.INCOME);
        transaction.setTransactionDate(java.time.LocalDate.now());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setCategory(null);
        transactionRepository.save(transaction);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));
    }

    private Account findOwnedAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Conta nao encontrada"));
        assertOwnedByCurrentUser(account);
        return account;
    }

    private Category findOwnedCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Categoria nao encontrada"));
        assertOwnedByCurrentUser(category);
        return category;
    }

    private void applyBalanceChange(Account account, TransactionType type, BigDecimal amount, boolean reverse) {
        BigDecimal currentBalance = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
        BigDecimal signedAmount = type == TransactionType.INCOME ? amount : amount.negate();
        if (reverse) {
            signedAmount = signedAmount.negate();
        }
        account.setBalance(currentBalance.add(signedAmount));
        account.setUpdatedAt(LocalDateTime.now());
    }

    private void assertOwnedByCurrentUser(Account account) {
        Long currentUserId = currentUserService.requireCurrentUserId();
        if (account.getUser() == null || !currentUserId.equals(account.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Recurso nao pertence ao usuario autenticado");
        }
    }

    private void assertOwnedByCurrentUser(Category category) {
        Long currentUserId = currentUserService.requireCurrentUserId();
        if (category.getUser() == null || !currentUserId.equals(category.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Recurso nao pertence ao usuario autenticado");
        }
    }

    private void assertOwnedByCurrentUser(Transaction transaction) {
        Long currentUserId = currentUserService.requireCurrentUserId();
        if (transaction.getUser() == null || !currentUserId.equals(transaction.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Recurso nao pertence ao usuario autenticado");
        }
    }
}


