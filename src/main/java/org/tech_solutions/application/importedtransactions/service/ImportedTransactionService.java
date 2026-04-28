package org.tech_solutions.application.importedtransactions.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.tech_solutions.application.importedtransactions.model.ImportedTransaction;
import org.tech_solutions.application.importedtransactions.repository.ImportedTransactionRepository;
import org.tech_solutions.application.imports.model.ImportFile;
import org.tech_solutions.application.imports.repository.ImportFileRepository;
import org.tech_solutions.application.security.CurrentUserService;
import org.tech_solutions.application.shared.exception.EntityNotFoundException;

import java.util.List;

@Service
public class ImportedTransactionService {

    private final ImportedTransactionRepository importedTransactionRepository;
    private final ImportFileRepository importFileRepository;
    private final CurrentUserService currentUserService;

    public ImportedTransactionService(
            ImportedTransactionRepository importedTransactionRepository,
            ImportFileRepository importFileRepository,
            CurrentUserService currentUserService
    ) {
        this.importedTransactionRepository = importedTransactionRepository;
        this.importFileRepository = importFileRepository;
        this.currentUserService = currentUserService;
    }

    public ImportedTransaction create(ImportedTransaction transaction, Long importId) {
        transaction.setImportFile(findImport(importId));
        transaction.setProcessed(false);
        return importedTransactionRepository.save(transaction);
    }

    public List<ImportedTransaction> listAll() {
        return importedTransactionRepository.findByImportFileUserId(currentUserService.requireCurrentUserId());
    }

    public List<ImportedTransaction> listByImport(Long importId) {
        findImport(importId);
        return importedTransactionRepository.findByImportFileId(importId);
    }

    public ImportedTransaction findById(Long id) {
        return importedTransactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transacao importada nao encontrada"));
    }

    public ImportedTransaction update(Long id, ImportedTransaction updated, Long importId) {
        ImportedTransaction current = findById(id);
        current.setImportFile(findImport(importId));
        current.setRawDescription(updated.getRawDescription());
        current.setRawAmount(updated.getRawAmount());
        current.setRawDate(updated.getRawDate());
        return importedTransactionRepository.save(current);
    }

    public void delete(Long id) {
        importedTransactionRepository.delete(findById(id));
    }

    private ImportFile findImport(Long importId) {
        ImportFile importFile = importFileRepository.findById(importId)
                .orElseThrow(() -> new EntityNotFoundException("Importacao nao encontrada"));
        Long currentUserId = currentUserService.requireCurrentUserId();
        if (importFile.getUser() == null || !currentUserId.equals(importFile.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Recurso nao pertence ao usuario autenticado");
        }
        return importFile;
    }
}


