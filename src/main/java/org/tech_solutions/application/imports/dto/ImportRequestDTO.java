package org.tech_solutions.application.imports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.tech_solutions.application.imports.enums.ImportStatus;

import java.math.BigInteger;

public record ImportRequestDTO(
        @NotNull(message = "'userId' nao pode ser nulo")
        BigInteger userId,
        @NotBlank(message = "'fileName' nao pode ser vazio")
        String fileName,
        ImportStatus status
) {
}

