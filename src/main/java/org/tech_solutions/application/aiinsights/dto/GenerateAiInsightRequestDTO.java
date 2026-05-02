package org.tech_solutions.application.aiinsights.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.tech_solutions.application.aiinsights.enums.InsightType;

public record GenerateAiInsightRequestDTO(
        @NotNull(message = "'insightType' nao pode ser nulo") InsightType insightType,
        @NotBlank(message = "'specification' nao pode ser vazio") String specification) {
}
