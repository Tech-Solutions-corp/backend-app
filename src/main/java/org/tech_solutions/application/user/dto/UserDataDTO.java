package org.tech_solutions.application.user.dto;

import java.time.LocalDateTime;

public record UserDataDTO(
        Long id,
        String name,
        String email,
        LocalDateTime created_at,
        LocalDateTime updated_at
) {
}
