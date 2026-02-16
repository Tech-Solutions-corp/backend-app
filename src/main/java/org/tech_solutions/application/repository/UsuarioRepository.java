package org.tech_solutions.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tech_solutions.application.model.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByTelefone(String telefone);
}
