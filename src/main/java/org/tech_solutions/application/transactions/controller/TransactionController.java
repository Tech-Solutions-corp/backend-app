package org.tech_solutions.application.transactions.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tech_solutions.application.transactions.dto.TransactionDataDTO;
import org.tech_solutions.application.transactions.dto.TransactionRequestDTO;
import org.tech_solutions.application.transactions.mapper.TransactionMapper;
import org.tech_solutions.application.transactions.model.Transaction;
import org.tech_solutions.application.transactions.service.TransactionService;

import java.util.List;

@RestController
@RequestMapping("api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionDataDTO> create(@Valid @RequestBody TransactionRequestDTO request) {
        Transaction created = transactionService.create(
                TransactionMapper.toModel(request),
                request.userId(),
                request.accountId(),
                request.categoryId()
        );
        return ResponseEntity.status(201).body(TransactionMapper.toDTO(created));
    }

    @GetMapping
    public ResponseEntity<List<TransactionDataDTO>> listAll() {
        List<Transaction> transactions = transactionService.listAll();
        return transactions.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(TransactionMapper.toDTO(transactions));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TransactionDataDTO>> listByUser(@PathVariable Long userId) {
        List<Transaction> transactions = transactionService.listByUser(userId);
        return transactions.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(TransactionMapper.toDTO(transactions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDataDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(TransactionMapper.toDTO(transactionService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionDataDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequestDTO request
    ) {
        Transaction updated = transactionService.update(
                id,
                TransactionMapper.toModel(request),
                request.userId(),
                request.accountId(),
                request.categoryId()
        );
        return ResponseEntity.ok(TransactionMapper.toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}


