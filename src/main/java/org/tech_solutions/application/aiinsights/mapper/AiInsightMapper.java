package org.tech_solutions.application.aiinsights.mapper;

import org.tech_solutions.application.aiinsights.dto.AiInsightDataDTO;
import org.tech_solutions.application.aiinsights.dto.AiInsightRequestDTO;
import org.tech_solutions.application.aiinsights.model.AiInsight;

import java.util.List;

public class AiInsightMapper {

    private AiInsightMapper() {
    }

    public static AiInsight toModel(AiInsightRequestDTO dto) {
        AiInsight insight = new AiInsight();
        insight.setInsightType(dto.insightType());
        insight.setContent(dto.content());
        return insight;
    }

    public static AiInsightDataDTO toDTO(AiInsight insight) {
        return new AiInsightDataDTO(
                insight.getId(),
                insight.getUser().getId(),
                insight.getInsightType(),
                insight.getContent(),
                insight.getGeneratedAt()
        );
    }

    public static List<AiInsightDataDTO> toDTO(List<AiInsight> insights) {
        return insights.stream().map(AiInsightMapper::toDTO).toList();
    }
}

