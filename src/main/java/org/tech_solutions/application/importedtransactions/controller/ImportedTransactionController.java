package org.tech_solutions.application.importedtransactions.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tech_solutions.application.importedtransactions.dto.ImportedTransactionDataDTO;
import org.tech_solutions.application.importedtransactions.dto.ImportedTransactionRequestDTO;
import org.tech_solutions.application.importedtransactions.mapper.ImportedTransactionMapper;
import org.tech_solutions.application.importedtransactions.model.ImportedTransaction;
import org.tech_solutions.application.importedtransactions.service.ImportedTransactionService;

import java.util.List;

@RestController
@RequestMapping("api/v1/imported-transactions")
public class ImportedTransactionController {

    private final ImportedTransactionService importedTransactionService;

    public ImportedTransactionController(ImportedTransactionService importedTransactionService) {
        this.importedTransactionService = importedTransactionService;
    }

    @PostMapping
    public ResponseEntity<ImportedTransactionDataDTO> create(@Valid @RequestBody ImportedTransactionRequestDTO request) {
        ImportedTransaction created = importedTransactionService.create(
                ImportedTransactionMapper.toModel(request),
                request.importId()
        );
        return ResponseEntity.status(201).body(ImportedTransactionMapper.toDTO(created));
    }

    @GetMapping
    public ResponseEntity<List<ImportedTransactionDataDTO>> listAll() {
        List<ImportedTransaction> transactions = importedTransactionService.listAll();
        return transactions.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(ImportedTransactionMapper.toDTO(transactions));
    }

    @GetMapping("/import/{importId}")
    public ResponseEntity<List<ImportedTransactionDataDTO>> listByImport(@PathVariable Long importId) {
        List<ImportedTransaction> transactions = importedTransactionService.listByImport(importId);
        return transactions.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(ImportedTransactionMapper.toDTO(transactions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImportedTransactionDataDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ImportedTransactionMapper.toDTO(importedTransactionService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ImportedTransactionDataDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ImportedTransactionRequestDTO request
    ) {
        ImportedTransaction updated = importedTransactionService.update(
                id,
                ImportedTransactionMapper.toModel(request),
                request.importId()
        );
        return ResponseEntity.ok(ImportedTransactionMapper.toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        importedTransactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}


