package org.tech_solutions.application.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CadastroUsuarioDto(
        @NotBlank( message = "'nome' não pode ser vazio" )
        String nome,
        @NotBlank( message = "'senha' não pode ser vazio" )
        String senha,
        @NotBlank( message = "'email' não pode ser vazio" )
        @Email   ( message = "email enviado possui valor inválido" )
        String email,
        @NotBlank( message = "'telefone' não pode ser vazio" )
        @Pattern( regexp = "\\d{11}", message = "telefone deve ter 11 dígitos, incluindo DDD")
        String telefone
) {
}
