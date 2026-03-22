package org.tech_solutions.application.controller.user;

import org.tech_solutions.application.controller.dto.UserRegisterDTO;
import org.tech_solutions.application.controller.dto.UserDataDTO;
import org.tech_solutions.application.model.User;

import java.util.List;

public class UserMapper {
    public static User toModel(UserRegisterDTO dto) {
        return new User(
                dto.name(),
                dto.password(),
                dto.email()
        );
    }

    public static UserDataDTO toDTO(User user) {
        return new UserDataDTO(
                user.getId(),
                user.getName(),
                user.getEmail()
                );
    }

    public static List<UserDataDTO> toDTO(List<User> user) {
        return user.stream().map(UserMapper::toDTO).toList();
    }
}
