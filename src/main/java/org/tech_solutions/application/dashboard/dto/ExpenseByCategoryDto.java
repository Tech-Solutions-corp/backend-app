package org.tech_solutions.application.dashboard.dto;

import java.math.BigDecimal;

public record ExpenseByCategoryDto(
        String category,
        BigDecimal totalAmount
) {
}
