package org.tech_solutions.application.controller.dto;

public record UsuarioDadosDTO(
        Long id,
        String nome,
        String email,
        String telefone
) {
}
