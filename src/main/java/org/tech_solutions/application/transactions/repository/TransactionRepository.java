package org.tech_solutions.application.transactions.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tech_solutions.application.transactions.enums.TransactionType;
import org.tech_solutions.application.transactions.model.Transaction;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserId(Long userId);

    List<Transaction> findByCategoryId(Long categoryId);

    @Modifying
    @Query("""
                DELETE FROM Transaction t
                WHERE t.account.id = :accountId
                  AND t.user.id = :userId
            """)
    void deleteByAccountIdAndUserId(
            @Param("accountId") Long accountId,
            @Param("userId") Long userId);

    @Query("""
                SELECT COALESCE(c.name, 'Sem categoria'), SUM(t.amount) as total
                FROM Transaction t
                LEFT JOIN t.category c
                WHERE t.user.id = :userId
                  AND t.transactionType = org.tech_solutions.application.transactions.enums.TransactionType.EXPENSE
                  AND t.transactionDate BETWEEN :start AND :end
                GROUP BY c.name
                ORDER BY total DESC
            """)
    List<Object[]> findExpensesByCategoryAndPeriod(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("""
                SELECT t.transactionType, SUM(t.amount)
                FROM Transaction t
                WHERE t.user.id = :userId
                  AND t.transactionDate BETWEEN :start AND :end
                GROUP BY t.transactionType
            """)
    List<Object[]> findIncomeVsExpense(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("""
                SELECT t FROM Transaction t
                WHERE t.user.id = :userId
                  AND t.transactionType = :type
                  AND t.transactionDate BETWEEN :start AND :end
                ORDER BY t.amount DESC
            """)
    List<Transaction> findTopExpenses(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("type") TransactionType type,
            Pageable pageable);
}
