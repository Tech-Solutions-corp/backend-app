package org.tech_solutions.application.monthlylimits.mapper;

import org.tech_solutions.application.monthlylimits.dto.MonthlyLimitDataDTO;
import org.tech_solutions.application.monthlylimits.dto.MonthlyLimitRequestDTO;
import org.tech_solutions.application.monthlylimits.model.MonthlyLimit;

import java.util.List;

public class MonthlyLimitMapper {

    private MonthlyLimitMapper() {
    }

    public static MonthlyLimit toModel(MonthlyLimitRequestDTO dto) {
        MonthlyLimit limit = new MonthlyLimit();
        limit.setReferenceMonth(dto.referenceMonth());
        limit.setAmount(dto.amount());
        return limit;
    }

    public static MonthlyLimitDataDTO toDTO(MonthlyLimit limit) {
        return new MonthlyLimitDataDTO(
                limit.getId(),
                limit.getUser().getId(),
                limit.getReferenceMonth(),
                limit.getAmount(),
                limit.getCreatedAt(),
                limit.getUpdatedAt());
    }

    public static List<MonthlyLimitDataDTO> toDTO(List<MonthlyLimit> limits) {
        return limits.stream().map(MonthlyLimitMapper::toDTO).toList();
    }
}