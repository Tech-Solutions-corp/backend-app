package org.tech_solutions.application.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequestDTO(
        @NotBlank(message = "'currentPassword' nao pode ser vazio") String currentPassword,
        @NotBlank(message = "'newPassword' nao pode ser vazio") @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,64}$", message = "'newPassword' deve conter no minimo 8 caracteres, com maiuscula, minuscula, numero e simbolo") String newPassword,
        @NotBlank(message = "'confirmNewPassword' nao pode ser vazio") String confirmNewPassword) {
}
