package org.tech_solutions.application.accounts.service;

import org.springframework.stereotype.Service;
import org.tech_solutions.application.accounts.model.Account;
import org.tech_solutions.application.accounts.repository.AccountRepository;
import org.tech_solutions.application.shared.exception.EntityNotFoundException;
import org.tech_solutions.application.user.model.User;
import org.tech_solutions.application.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public Account create(Account account, Long userId) {
        User user = findUser(userId);
        account.setUser(user);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    public List<Account> listAll() {
        return accountRepository.findAll();
    }

    public List<Account> listByUser(Long userId) {
        findUser(userId);
        return accountRepository.findByUserId(userId);
    }

    public Account findById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conta nao encontrada"));
    }

    public Account update(Long id, Account updated, Long userId) {
        Account current = findById(id);
        User user = findUser(userId);

        current.setUser(user);
        current.setName(updated.getName());
        current.setType(updated.getType());
        current.setBalance(updated.getBalance());
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
}

