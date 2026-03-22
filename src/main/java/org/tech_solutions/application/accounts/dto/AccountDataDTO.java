package org.tech_solutions.application.accounts.dto;

import org.tech_solutions.application.accounts.enums.Type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

public record AccountDataDTO(
        BigInteger id,
        BigInteger userId,
        String name,
        Type type,
        BigDecimal balance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

