package org.tech_solutions.application.security.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.tech_solutions.application.repository.UsuarioRepository;

@Service
public class AuthManager implements UserDetailsService {
    private final UsuarioRepository repository;

    public AuthManager(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByEmail(username)
                .orElseThrow(
                        () -> new UsernameNotFoundException("Usuario não encontrado para o email: "+ username)
                );

    }
}
