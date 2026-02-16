package org.tech_solutions.application.security.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.tech_solutions.application.security.TokenManager;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/* =====================================================================================
    Classe que implementa um filtro acionado a cada requisição recebida pelo sistema
    que valida a identidade do cliente
===================================================================================== */

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthTokenFilter.class);

    private final TokenManager tokenManager;

    public AuthTokenFilter(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            this.obterToken(request)
                    .flatMap(tokenManager::obterUsernameDeToken)
                    .ifPresent(this::autenticarUsuario);

        } catch (Exception e) {
            LOGGER.error("Erro ao processar autenticação: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    /* ===================================================================
        Verifica existência de token presente no header
        e o retorna formatado sem a identificação 'Bearer ' em seu início
    =================================================================== */
    private Optional<String> obterToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        token = token.trim();

        boolean bearerToken = token.startsWith("Bearer ");
        if (!bearerToken) {
            return Optional.empty();
        }

        String tokenFormatado = token.substring(7);
        return Optional.of(tokenFormatado);
    }

    private void autenticarUsuario(String username) {
        var authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                Collections.emptyList() // Sistema ainda não trabalha com roles, caso for implementado, alterar para ser refletido
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}