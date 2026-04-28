package org.tech_solutions.application.accounts.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.tech_solutions.application.accounts.model.Account;
import org.tech_solutions.application.accounts.repository.AccountRepository;
import org.tech_solutions.application.security.CurrentUserService;
import org.tech_solutions.application.shared.exception.EntityNotFoundException;
import org.tech_solutions.application.user.model.User;
import org.tech_solutions.application.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository, CurrentUserService currentUserService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public Account create(Account account, Long userId) {
        User user = currentUserService.requireCurrentUser();
        account.setUser(user);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        return accountRepository.save(account);
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

    public void delete(Long id) {
        Account current = findById(id);
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

