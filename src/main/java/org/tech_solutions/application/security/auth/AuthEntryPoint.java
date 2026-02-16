package org.tech_solutions.application.security.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.tech_solutions.application.controller.dto.ErrorDTO;

import java.io.IOException;

/* ==============================================================
    Esta classe intercepta requisições consideradas inválidas
    relacionadas ao token JWT recebido pelo sistema e retorna
    um DTO com o tratamento adequado
============================================================== */
@Component
public class AuthEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper mapper;

    public AuthEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ErrorDTO errorResponse = new ErrorDTO(
                HttpStatus.UNAUTHORIZED.value(),
                "Acesso nao autorizado. Token invalido ou ausente"
        );

        response.getWriter().write(mapper.writeValueAsString(errorResponse));
     }
}
