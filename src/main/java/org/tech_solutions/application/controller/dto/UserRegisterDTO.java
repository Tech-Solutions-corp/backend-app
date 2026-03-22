package org.tech_solutions.application.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRegisterDTO(
        @NotBlank( message = "'nome' nao pode ser vazio" )
        String name,
        @NotBlank( message = "'senha' nao pode ser vazio" )
        String password,
        @NotBlank( message = "'email' nao pode ser vazio" )
        @Email   ( message = "email enviado possui valor invalido" )
        String email
) {
}
