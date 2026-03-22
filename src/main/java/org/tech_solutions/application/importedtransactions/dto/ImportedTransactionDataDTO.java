package org.tech_solutions.application.importedtransactions.dto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

public record ImportedTransactionDataDTO(
        BigInteger id,
        BigInteger importId,
        String rawDescription,
        BigDecimal rawAmount,
        LocalDate rawDate,
        Boolean processed
) {
}

