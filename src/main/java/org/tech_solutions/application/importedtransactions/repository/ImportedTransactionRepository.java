package org.tech_solutions.application.importedtransactions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tech_solutions.application.importedtransactions.model.ImportedTransaction;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ImportedTransactionRepository extends JpaRepository<ImportedTransaction, Long> {
    List<ImportedTransaction> findByImportFileId(Long importId);
    List<ImportedTransaction> findByImportFileIdAndProcessedFalseOrderByIdAsc(Long importId);
    List<ImportedTransaction> findByImportFileUserId(Long userId);
    List<ImportedTransaction> findByAccountId(Long accountId);
    List<ImportedTransaction> findByCategoryId(Long categoryId);
    


    @Query("""
    SELECT SUM(it.rawAmount), COUNT(it)
    FROM ImportedTransaction it
    JOIN it.importFile imp
    JOIN imp.user u
    WHERE u.id = :userId
      AND it.rawDate BETWEEN :start AND :end
      AND it.processed = false
""")
    List<Object[]> findRawSummaryByUser(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
