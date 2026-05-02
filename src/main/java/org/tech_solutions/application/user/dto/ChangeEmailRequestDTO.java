package org.tech_solutions.application.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangeEmailRequestDTO(
        @NotBlank(message = "'currentPassword' nao pode ser vazio") String currentPassword,
        @NotBlank(message = "'newEmail' nao pode ser vazio") @Email(message = "Email enviado possui valor invalido") String newEmail) {
}
