package org.tech_solutions.application.controller.usuario;

import org.tech_solutions.application.controller.dto.CadastroUsuarioDto;
import org.tech_solutions.application.controller.dto.UsuarioDadosDTO;
import org.tech_solutions.application.model.Usuario;

import java.util.List;

public class UsuarioMapper {
    public static Usuario toModel(CadastroUsuarioDto dto) {
        return new Usuario(
                dto.nome(),
                dto.senha(),
                dto.email(),
                dto.telefone()
        );
    }

    public static UsuarioDadosDTO toDTO(Usuario usuario) {
        return new UsuarioDadosDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone()
                );
    }

    public static List<UsuarioDadosDTO> toDTO(List<Usuario> usuario) {
        return usuario.stream().map(UsuarioMapper::toDTO).toList();
    }
}
