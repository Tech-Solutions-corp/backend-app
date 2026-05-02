package org.tech_solutions.application.monthlylimits.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyLimitRequestDTO(
        @NotNull(message = "'userId' nao pode ser nulo") Long userId,
        @NotNull(message = "'referenceMonth' nao pode ser nulo") LocalDate referenceMonth,
        @NotNull(message = "'amount' nao pode ser nulo") @DecimalMin(value = "0.01", message = "'amount' deve ser maior que zero") BigDecimal amount) {
}