package org.tech_solutions.application.categories.dto;

import org.tech_solutions.application.categories.enums.CategoryType;

import java.math.BigInteger;
import java.time.LocalDateTime;

public record CategoryDataDTO(
        BigInteger id,
        BigInteger userId,
        String name,
        CategoryType type,
        LocalDateTime createdAt
) {
}

