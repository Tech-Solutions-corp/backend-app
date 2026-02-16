package org.tech_solutions.application.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.tech_solutions.application.controller.dto.LoginDTO;
import org.tech_solutions.application.controller.dto.UsuarioLogadoDTO;
import org.tech_solutions.application.model.Usuario;
import org.tech_solutions.application.security.TokenManager;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final TokenManager tokenManager;

    public AuthService(AuthenticationManager authenticationManager, TokenManager tokenManager) {
        this.authenticationManager = authenticationManager;
        this.tokenManager = tokenManager;
    }

    public UsuarioLogadoDTO autenticar(LoginDTO request) {
        var usuarioAutenticar = new UsernamePasswordAuthenticationToken(
                request.email(),
                request.senha()
        );

        Authentication resultadoAutenticacao = authenticationManager.authenticate(usuarioAutenticar);
        Usuario usuarioAutenticado = (Usuario) resultadoAutenticacao.getPrincipal();

        String token = tokenManager.gerarToken(usuarioAutenticado.getUsername());

        return new UsuarioLogadoDTO(usuarioAutenticado.getId(), token);
    }
}
