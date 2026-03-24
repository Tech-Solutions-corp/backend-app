package org.tech_solutions.application.importedtransactions.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ImportedTransactionDataDTO(
        Long id,
        Long importId,
        String rawDescription,
        BigDecimal rawAmount,
        LocalDate rawDate,
        Boolean processed
) {
}


