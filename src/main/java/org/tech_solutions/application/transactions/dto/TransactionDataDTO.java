package org.tech_solutions.application.transactions.dto;

import org.tech_solutions.application.transactions.enums.TransactionType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionDataDTO(
        BigInteger id,
        BigInteger userId,
        BigInteger accountId,
        BigInteger categoryId,
        String transactionDescription,
        BigDecimal amount,
        LocalDate transactionDate,
        TransactionType transactionType,
        LocalDateTime createdAt
) {
}

