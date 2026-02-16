package org.tech_solutions.application.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginDTO(
        @NotBlank( message = "'email' não pode ser vazio" )
        String email,
        @NotBlank( message = "'senha' não pode ser vazio" )
        String senha
) {
}
