package org.tech_solutions.application.accounts.dto;

import org.tech_solutions.application.accounts.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountDataDTO(
        Long id,
        Long userId,
        String name,
        Type type,
        BigDecimal balance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}


