package org.tech_solutions.application.imports.dto;

import org.tech_solutions.application.imports.enums.ImportStatus;

import java.math.BigInteger;
import java.time.LocalDateTime;

public record ImportDataDTO(
        BigInteger id,
        BigInteger userId,
        String fileName,
        LocalDateTime importedAt,
        ImportStatus status
) {
}

