package org.tech_solutions.application.user.dto;

import java.math.BigInteger;

public record UserLogedDTO(
        BigInteger id,
        String token
) {
}
