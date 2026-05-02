package org.tech_solutions.application.monthlylimits.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MonthlyLimitDataDTO(
        Long id,
        Long userId,
        LocalDate referenceMonth,
        BigDecimal amount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}