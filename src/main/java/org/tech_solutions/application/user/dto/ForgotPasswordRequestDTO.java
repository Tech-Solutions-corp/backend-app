package org.tech_solutions.application.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequestDTO(
        @NotBlank(message = "'email' nao pode ser vazio")
        @Email(message = "email enviado possui valor invalido")
        String email
) {
}
