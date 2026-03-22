package org.tech_solutions.application.shared.dto;

import java.time.Instant;

public record ErrorDTO(
        int errorCode,
        String message,
        Instant timestamp
) {
    public ErrorDTO(int errorCode, String message) {
        this(errorCode, message, Instant.now());
    }
}
