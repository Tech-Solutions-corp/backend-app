package org.tech_solutions.application.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.tech_solutions.application.controller.dto.LoginDTO;
import org.tech_solutions.application.controller.dto.UserLogedDTO;
import org.tech_solutions.application.model.User;
import org.tech_solutions.application.security.TokenManager;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final TokenManager tokenManager;

    public AuthService(AuthenticationManager authenticationManager, TokenManager tokenManager) {
        this.authenticationManager = authenticationManager;
        this.tokenManager = tokenManager;
    }

    public UserLogedDTO autenticate(LoginDTO request) {
        var userAuthenticate = new UsernamePasswordAuthenticationToken(
                request.email(),
                request.password()
        );

        Authentication authenticationResult = authenticationManager.authenticate(userAuthenticate);
        User userAuthenticated = (User) authenticationResult.getPrincipal();

        String token = tokenManager.gerarToken(userAuthenticated.getUsername());

        return new UserLogedDTO(userAuthenticated.getId(), token);
    }
}
