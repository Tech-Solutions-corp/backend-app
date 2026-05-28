package org.tech_solutions.application.accounts.service;
import org.tech_solutions.application.importedtransactions.repository.ImportedTransactionRepository;
import org.tech_solutions.application.importedtransactions.model.ImportedTransaction;
import org.tech_solutions.application.categories.model.Category;
import org.tech_solutions.application.categories.repository.CategoryRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.tech_solutions.application.accounts.model.Account;
import org.tech_solutions.application.accounts.repository.AccountRepository;
import org.tech_solutions.application.security.CurrentUserService;
import org.tech_solutions.application.shared.exception.EntityNotFoundException;
import org.tech_solutions.application.transactions.repository.TransactionRepository;
import org.tech_solutions.application.user.model.User;
import org.tech_solutions.application.user.repository.UserRepository;
import org.tech_solutions.application.transactions.service.TransactionService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;
    private final ImportedTransactionRepository importedTransactionRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionService transactionService;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            CurrentUserService currentUserService,
            ImportedTransactionRepository importedTransactionRepository,
            CategoryRepository categoryRepository,
            TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
        this.importedTransactionRepository = importedTransactionRepository;
        this.categoryRepository = categoryRepository;
        this.transactionService = transactionService;
    }

    public Account create(Account account, Long userId) {
        User user = currentUserService.requireCurrentUser();
        account.setUser(user);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        Account savedAccount = accountRepository.save(account);
        if (account.getBalance() != null && account.getBalance().compareTo(java.math.BigDecimal.ZERO) > 0) {
            transactionService.createInitialBalance(savedAccount, account.getBalance());
        }
        return savedAccount;
    }

    public List<Account> listAll() {
        return accountRepository.findByUserId(currentUserService.requireCurrentUserId());
    }

    public List<Account> listByUser(Long userId) {
        return accountRepository.findByUserId(currentUserService.requireCurrentUserId());
    }

    public Account findById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conta nao encontrada"));
        assertOwnedByCurrentUser(account);
        return account;
    }

    public Account update(Long id, Account updated, Long userId) {
        Account current = findById(id);
        User user = currentUserService.requireCurrentUser();

        current.setUser(user);
        current.setName(updated.getName());
        current.setType(updated.getType());
        current.setUpdatedAt(LocalDateTime.now());

        return accountRepository.save(current);
    }

    @Transactional
    public void delete(Long id) {
        delete(id, null);
    }

    @Transactional
    public void delete(Long id, Long reassignToAccountId) {
        Account current = findById(id);
        Long currentUserId = currentUserService.requireCurrentUserId();

        Account reassignAccount = null;
        if (reassignToAccountId != null) {
            reassignAccount = accountRepository.findById(reassignToAccountId)
                    .orElseThrow(() -> new EntityNotFoundException("Conta de reatribuição nao encontrada"));
            assertOwnedByCurrentUser(reassignAccount);
            if (reassignAccount.getId().equals(current.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conta de reatribuição deve ser diferente");
            }
        }

        // Reatribuir ou excluir transações principais
        if (reassignAccount != null) {
            var transactions = transactionRepository.findByUserId(currentUserId)
                    .stream().filter(t -> t.getAccount().getId().equals(current.getId())).toList();
            for (var t : transactions) {
                t.setAccount(reassignAccount);
            }
            if (!transactions.isEmpty())
                transactionRepository.saveAll(transactions);
        } else {
            transactionRepository.deleteByAccountIdAndUserId(current.getId(), currentUserId);
        }

        // Reatribuir ou excluir transações importadas
        if (reassignAccount != null) {
            var imported = importedTransactionRepository.findByAccountId(current.getId());
            for (ImportedTransaction it : imported) {
                it.setAccount(reassignAccount);
            }
            if (!imported.isEmpty())
                importedTransactionRepository.saveAll(imported);
        } else {
            var imported = importedTransactionRepository.findByAccountId(current.getId());
            if (!imported.isEmpty())
                importedTransactionRepository.deleteAll(imported);
        }

        accountRepository.delete(current);
    }

    @Transactional
    public void delete(Long id, Long reassignToAccountId, Long reassignCategoryId) {
        Account current = findById(id);
        Long currentUserId = currentUserService.requireCurrentUserId();

        Account reassignAccount = null;
        if (reassignToAccountId != null) {
            reassignAccount = accountRepository.findById(reassignToAccountId)
                    .orElseThrow(() -> new org.tech_solutions.application.shared.exception.EntityNotFoundException("Conta de reatribuição nao encontrada"));
            assertOwnedByCurrentUser(reassignAccount);
            if (reassignAccount.getId().equals(current.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conta de reatribuição deve ser diferente");
            }
        }

        Category reassignCategory = null;
        if (reassignCategoryId != null) {
            reassignCategory = categoryRepository.findById(reassignCategoryId)
                    .orElseThrow(() -> new org.tech_solutions.application.shared.exception.EntityNotFoundException("Categoria de reatribuição nao encontrada"));
            Long currentUser = currentUserService.requireCurrentUserId();
            if (reassignCategory.getUser() == null || !currentUser.equals(reassignCategory.getUser().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Categoria de reatribuição nao pertence ao usuario");
            }
        }

        // If reassignAccount provided, reassign transactions and optionally change their category
        if (reassignAccount != null) {
            var transactions = transactionRepository.findByUserId(currentUserId)
                    .stream().filter(t -> t.getAccount().getId().equals(current.getId())).toList();
            for (var t : transactions) {
                t.setAccount(reassignAccount);
                if (reassignCategory != null) {
                    t.setCategory(reassignCategory);
                }
            }
            if (!transactions.isEmpty())
                transactionRepository.saveAll(transactions);
        } else {
            transactionRepository.deleteByAccountIdAndUserId(current.getId(), currentUserId);
        }

        // imported transactions
        if (reassignAccount != null) {
            var imported = importedTransactionRepository.findByAccountId(current.getId());
            for (ImportedTransaction it : imported) {
                it.setAccount(reassignAccount);
                if (reassignCategory != null) {
                    it.setCategory(reassignCategory);
                }
            }
            if (!imported.isEmpty())
                importedTransactionRepository.saveAll(imported);
        } else {
            var imported = importedTransactionRepository.findByAccountId(current.getId());
            if (!imported.isEmpty())
                importedTransactionRepository.deleteAll(imported);
        }

        accountRepository.delete(current);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));
    }

    private void assertOwnedByCurrentUser(Account account) {
        Long currentUserId = currentUserService.requireCurrentUserId();
        if (account.getUser() == null || !currentUserId.equals(account.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Recurso nao pertence ao usuario autenticado");
        }
    }
}
