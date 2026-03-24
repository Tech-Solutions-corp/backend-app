package org.tech_solutions.application.transactions.dto;

import org.tech_solutions.application.transactions.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionDataDTO(
        Long id,
        Long userId,
        Long accountId,
        Long categoryId,
        String transactionDescription,
        BigDecimal amount,
        LocalDate transactionDate,
        TransactionType transactionType,
        LocalDateTime createdAt
) {
}


