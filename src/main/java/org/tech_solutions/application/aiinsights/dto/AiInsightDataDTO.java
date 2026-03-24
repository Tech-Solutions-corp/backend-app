package org.tech_solutions.application.aiinsights.dto;

import org.tech_solutions.application.aiinsights.enums.InsightType;

import java.time.LocalDateTime;

public record AiInsightDataDTO(
        Long id,
        Long userId,
        InsightType insightType,
        String content,
        LocalDateTime generatedAt
) {
}


