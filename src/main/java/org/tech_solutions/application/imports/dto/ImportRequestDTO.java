package org.tech_solutions.application.imports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.tech_solutions.application.imports.enums.ImportStatus;


public record ImportRequestDTO(
        @NotNull(message = "'userId' nao pode ser nulo")
        Long userId,
        @NotBlank(message = "'fileName' nao pode ser vazio")
        String fileName,
        ImportStatus status
) {
}


