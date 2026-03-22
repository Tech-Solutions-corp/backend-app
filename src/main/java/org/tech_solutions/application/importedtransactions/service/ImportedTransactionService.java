package org.tech_solutions.application.importedtransactions.service;

import org.springframework.stereotype.Service;
import org.tech_solutions.application.importedtransactions.model.ImportedTransaction;
import org.tech_solutions.application.importedtransactions.repository.ImportedTransactionRepository;
import org.tech_solutions.application.imports.model.ImportFile;
import org.tech_solutions.application.imports.repository.ImportFileRepository;
import org.tech_solutions.application.shared.exception.EntityNotFoundException;

import java.math.BigInteger;
import java.util.List;

@Service
public class ImportedTransactionService {

    private final ImportedTransactionRepository importedTransactionRepository;
    private final ImportFileRepository importFileRepository;

    public ImportedTransactionService(
            ImportedTransactionRepository importedTransactionRepository,
            ImportFileRepository importFileRepository
    ) {
        this.importedTransactionRepository = importedTransactionRepository;
        this.importFileRepository = importFileRepository;
    }

    public ImportedTransaction create(ImportedTransaction transaction, BigInteger importId) {
        transaction.setImportFile(findImport(importId));
        if (transaction.getProcessed() == null) {
            transaction.setProcessed(false);
        }
        return importedTransactionRepository.save(transaction);
    }

    public List<ImportedTransaction> listAll() {
        return importedTransactionRepository.findAll();
    }

    public List<ImportedTransaction> listByImport(BigInteger importId) {
        findImport(importId);
        return importedTransactionRepository.findByImportFileId(importId);
    }

    public ImportedTransaction findById(BigInteger id) {
        return importedTransactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transacao importada nao encontrada"));
    }

    public ImportedTransaction update(BigInteger id, ImportedTransaction updated, BigInteger importId) {
        ImportedTransaction current = findById(id);
        current.setImportFile(findImport(importId));
        current.setRawDescription(updated.getRawDescription());
        current.setRawAmount(updated.getRawAmount());
        current.setRawDate(updated.getRawDate());
        current.setProcessed(updated.getProcessed() == null ? current.getProcessed() : updated.getProcessed());
        return importedTransactionRepository.save(current);
    }

    public void delete(BigInteger id) {
        importedTransactionRepository.delete(findById(id));
    }

    private ImportFile findImport(BigInteger importId) {
        return importFileRepository.findById(importId)
                .orElseThrow(() -> new EntityNotFoundException("Importacao nao encontrada"));
    }
}

