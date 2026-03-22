package org.tech_solutions.application.user.dto;

import java.math.BigInteger;
import java.time.LocalDateTime;

public record UserDataDTO(
        BigInteger id,
        String name,
        String email,
        LocalDateTime created_at,
        LocalDateTime updated_at
) {
}
