package org.tech_solutions.application.aiinsights.dto;

import org.tech_solutions.application.aiinsights.enums.InsightType;

import java.math.BigInteger;
import java.time.LocalDateTime;

public record AiInsightDataDTO(
        BigInteger id,
        BigInteger userId,
        InsightType insightType,
        String content,
        LocalDateTime generatedAt
) {
}

