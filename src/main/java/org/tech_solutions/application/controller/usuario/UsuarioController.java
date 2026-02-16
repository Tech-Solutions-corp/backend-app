package org.tech_solutions.application.controller.usuario;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tech_solutions.application.controller.dto.CadastroUsuarioDto;
import org.tech_solutions.application.controller.dto.UsuarioDadosDTO;
import org.tech_solutions.application.controller.dto.LoginDTO;
import org.tech_solutions.application.controller.dto.UsuarioLogadoDTO;
import org.tech_solutions.application.model.Usuario;
import org.tech_solutions.application.service.AuthService;
import org.tech_solutions.application.service.UsuarioService;

import java.util.List;

@RestController
@RequestMapping("/v1/usuarios")
public class UsuarioController {
    private final AuthService authService;
    private final UsuarioService usuarioService;

    public UsuarioController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/auth")
    public ResponseEntity<UsuarioLogadoDTO> login(@Valid @RequestBody LoginDTO request) {
        UsuarioLogadoDTO response = authService.autenticar(request);
        return ResponseEntity.status(200).body(response);
    }

    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioDadosDTO> cadastrar(@Valid @RequestBody CadastroUsuarioDto request) {
        Usuario usuario = usuarioService.cadastrar(UsuarioMapper.toModel(request));
        return ResponseEntity.status(201).body(UsuarioMapper.toDTO(usuario));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDadosDTO>> listarTodos() {
        List<Usuario> usuariosEncontrados = usuarioService.listarTodos();

        return usuariosEncontrados.isEmpty() ?
                ResponseEntity.status(204).build()
             :  ResponseEntity.status(200).body(UsuarioMapper.toDTO(usuariosEncontrados));
    }
}
