package org.tech_solutions.application.imports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para requisição de importação de arquivo.
 * 
 * IMPORTANTE: Campos como 'descrição' não serão registrados como VALOR da transação.
 * Eles serão processados apenas como informações de contexto/metadata para:
 * - Classificação automática de categorias
 * - Histórico e rastreabilidade
 * 
 * Os valores das transações são registrados com base em:
 * - Valor monetário (amount)
 * - Data da transação (date)
 * - Tipo de transação (type)
 */
public record ImportRequestDTO(
        @NotNull(message = "'userId' nao pode ser nulo")
        Long userId,
        @NotBlank(message = "'fileName' nao pode ser vazio")
        String fileName
) {
}


