package org.tech_solutions.application.service;

import org.springframework.stereotype.Service;
import org.tech_solutions.application.exception.EntidadeExistenteException;
import org.tech_solutions.application.model.Usuario;
import org.tech_solutions.application.repository.UsuarioRepository;

import java.util.List;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public Usuario cadastrar(Usuario novoUsuario) {
        if(repository.existsByEmail(novoUsuario.getEmail())) {
            throw new EntidadeExistenteException("Usuário já existente no sistema para esse email");
        }

        if(repository.existsByTelefone(novoUsuario.getTelefone())) {
            throw new EntidadeExistenteException("Usuário já existente no sistema para esse telefone");
        }

        return repository.save(novoUsuario);
    }

    public List<Usuario> listarTodos() {
        return repository.findAll();
    }
}
