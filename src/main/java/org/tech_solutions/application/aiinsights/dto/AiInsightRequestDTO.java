package org.tech_solutions.application.aiinsights.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.tech_solutions.application.aiinsights.enums.InsightType;

import java.math.BigInteger;

public record AiInsightRequestDTO(
        @NotNull(message = "'userId' nao pode ser nulo")
        BigInteger userId,
        @NotNull(message = "'insightType' nao pode ser nulo")
        InsightType insightType,
        @NotBlank(message = "'content' nao pode ser vazio")
        String content
) {
}

