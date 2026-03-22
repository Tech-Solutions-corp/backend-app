package org.tech_solutions.application.accounts.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.tech_solutions.application.accounts.enums.Type;

import java.math.BigDecimal;
import java.math.BigInteger;

public record AccountRequestDTO(
        @NotNull(message = "'userId' nao pode ser nulo")
        BigInteger userId,
        @NotBlank(message = "'name' nao pode ser vazio")
        String name,
        @NotNull(message = "'type' nao pode ser nulo")
        Type type,
        @NotNull(message = "'balance' nao pode ser nulo")
        @DecimalMin(value = "0.0", message = "'balance' nao pode ser negativo")
        BigDecimal balance
) {
}

