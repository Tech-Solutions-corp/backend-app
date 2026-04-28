package org.tech_solutions.application.user.dto;

import java.time.LocalDateTime;

public record UserDataDTO(
        Long id,
        String name,
        String email,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

