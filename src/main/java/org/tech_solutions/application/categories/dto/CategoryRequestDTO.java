package org.tech_solutions.application.categories.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.tech_solutions.application.categories.enums.CategoryType;


public record CategoryRequestDTO(
        @NotNull(message = "'userId' nao pode ser nulo")
        Long userId,
        @NotBlank(message = "'name' nao pode ser vazio")
        String name,
        @NotNull(message = "'type' nao pode ser nulo")
        CategoryType type
) {
}


