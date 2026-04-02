package org.tech_solutions.application.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRegisterDTO(
        @NotBlank( message = "'name' nao pode ser vazio" )
        String name,
        @NotBlank( message = "'password' nao pode ser vazio" )
        String password,
        @NotBlank( message = "'email' nao pode ser vazio" )
        @Email   ( message = "email enviado possui valor invalido" )
        String email
) {
}
