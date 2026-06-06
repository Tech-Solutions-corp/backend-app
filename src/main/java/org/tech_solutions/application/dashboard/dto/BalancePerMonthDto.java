package org.tech_solutions.application.dashboard.dto;

import java.math.BigDecimal;

public record BalancePerMonthDto(
        Integer month,
        Integer year,
        BigDecimal income,
        BigDecimal expense
) {
}
