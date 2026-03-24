package org.tech_solutions.application.categories.dto;

import org.tech_solutions.application.categories.enums.CategoryType;

import java.time.LocalDateTime;

public record CategoryDataDTO(
        Long id,
        Long userId,
        String name,
        CategoryType type,
        LocalDateTime createdAt
) {
}


