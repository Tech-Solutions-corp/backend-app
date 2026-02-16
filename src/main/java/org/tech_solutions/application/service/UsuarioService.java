package org.tech_solutions.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.tech_solutions.application.exception.EntidadeExistenteException;
import org.tech_solutions.application.model.Usuario;
import org.tech_solutions.application.repository.UsuarioRepository;

import java.util.List;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario cadastrar(Usuario novoUsuario) {
        if(repository.existsByEmail(novoUsuario.getEmail())) {
            throw new EntidadeExistenteException("Usuário já existente no sistema para esse email");
        }
        novoUsuario.setSenha(passwordEncoder.encode(novoUsuario.getPassword()));
        return repository.save(novoUsuario);
    }

    public List<Usuario> listarTodos() {
        return repository.findAll();
    }
}
