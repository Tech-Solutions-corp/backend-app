package org.tech_solutions.application.importedtransactions.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

public record ImportedTransactionRequestDTO(
        @NotNull(message = "'importId' nao pode ser nulo")
        BigInteger importId,
        String rawDescription,
        BigDecimal rawAmount,
        LocalDate rawDate,
        Boolean processed
) {
}

