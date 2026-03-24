package org.tech_solutions.application.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequestDTO(
        @NotBlank(message = "'token' nao pode ser vazio")
        String token,
        @NotBlank(message = "'newPassword' nao pode ser vazio")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,64}$",
                message = "'newPassword' deve conter no minimo 8 caracteres, com maiuscula, minuscula, numero e simbolo"
        )
        String newPassword
) {
}
