package org.tech_solutions.application.importedtransactions.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ImportedTransactionRequestDTO(
        @NotNull(message = "'importId' nao pode ser nulo")
        Long importId,
        String rawDescription,
        BigDecimal rawAmount,
        LocalDate rawDate
) {
}


