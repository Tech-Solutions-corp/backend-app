package org.tech_solutions.application.monthlylimits.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tech_solutions.application.monthlylimits.model.MonthlyLimit;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MonthlyLimitRepository extends JpaRepository<MonthlyLimit, Long> {
    List<MonthlyLimit> findByUserIdOrderByReferenceMonthDesc(Long userId);

    Optional<MonthlyLimit> findByUserIdAndReferenceMonth(Long userId, LocalDate referenceMonth);
}