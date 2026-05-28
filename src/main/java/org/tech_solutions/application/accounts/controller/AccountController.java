package org.tech_solutions.application.accounts.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tech_solutions.application.accounts.dto.AccountDataDTO;
import org.tech_solutions.application.accounts.dto.AccountRequestDTO;
import org.tech_solutions.application.accounts.mapper.AccountMapper;
import org.tech_solutions.application.accounts.model.Account;
import org.tech_solutions.application.accounts.service.AccountService;

import java.util.List;

@RestController
@RequestMapping("api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountDataDTO> create(@Valid @RequestBody AccountRequestDTO request) {
        Account created = accountService.create(AccountMapper.toModel(request), request.userId());
        return ResponseEntity.status(201).body(AccountMapper.toDTO(created));
    }

    @GetMapping
    public ResponseEntity<List<AccountDataDTO>> listAll() {
        List<Account> accounts = accountService.listAll();
        return accounts.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(AccountMapper.toDTO(accounts));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountDataDTO>> listByUser(@PathVariable Long userId) {
        List<Account> accounts = accountService.listByUser(userId);
        return accounts.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(AccountMapper.toDTO(accounts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDataDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(AccountMapper.toDTO(accountService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDataDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody AccountRequestDTO request) {
        Account updated = accountService.update(id, AccountMapper.toModel(request), request.userId());
        return ResponseEntity.ok(AccountMapper.toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam(value = "reassignToAccountId", required = false) Long reassignToAccountId,
            @RequestParam(value = "reassignCategoryId", required = false) Long reassignCategoryId) {
        if (reassignToAccountId != null || reassignCategoryId != null) {
            accountService.delete(id, reassignToAccountId, reassignCategoryId);
        } else {
            accountService.delete(id);
        }
        return ResponseEntity.noContent().build();
    }
}
