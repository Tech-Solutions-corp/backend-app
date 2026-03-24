package org.tech_solutions.application.imports.dto;

import org.tech_solutions.application.imports.enums.ImportStatus;

import java.time.LocalDateTime;

public record ImportDataDTO(
        Long id,
        Long userId,
        String fileName,
        LocalDateTime importedAt,
        ImportStatus status
) {
}


