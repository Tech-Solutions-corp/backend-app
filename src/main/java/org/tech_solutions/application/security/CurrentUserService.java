package org.tech_solutions.application.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.tech_solutions.application.user.model.User;
import org.tech_solutions.application.user.repository.UserRepository;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireCurrentUser() {
        String email = requireCurrentEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado nao encontrado"));
    }

    public Long requireCurrentUserId() {
        return requireCurrentUser().getId();
    }

    public String requireCurrentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticacao ausente");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof String username) {
            return username;
        }

        if (principal instanceof User user) {
            return user.getUsername();
        }

        return principal.toString();
    }

    public int requireCurrentPasswordResetVersion() {
        Integer version = requireCurrentUser().getPasswordResetVersion();
        return version == null ? 0 : version;
    }
}