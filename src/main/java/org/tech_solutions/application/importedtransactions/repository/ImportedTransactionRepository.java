package org.tech_solutions.application.importedtransactions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tech_solutions.application.importedtransactions.model.ImportedTransaction;

import java.util.List;

@Repository
public interface ImportedTransactionRepository extends JpaRepository<ImportedTransaction, Long> {
    List<ImportedTransaction> findByImportFileId(Long importId);
}


