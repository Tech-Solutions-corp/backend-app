package org.tech_solutions.application.transactions.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.tech_solutions.application.transactions.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequestDTO(
        @NotNull(message = "'userId' nao pode ser nulo")
        Long userId,
        @NotNull(message = "'accountId' nao pode ser nulo")
        Long accountId,
        Long categoryId,
        String transactionDescription,
        @NotNull(message = "'amount' nao pode ser nulo")
        BigDecimal amount,
        @NotNull(message = "'transactionDate' nao pode ser nulo")
        LocalDate transactionDate,
        @NotNull(message = "'transactionType' nao pode ser nulo")
        TransactionType transactionType
) {
}


