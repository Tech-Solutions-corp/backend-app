package org.tech_solutions.application.security.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.tech_solutions.application.repository.UserRepository;

@Service
public class AuthManager implements UserDetailsService {
    private final UserRepository repository;

    public AuthManager(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByEmail(username)
                .orElseThrow(
                        () -> new UsernameNotFoundException("Usuario nao encontrado para o email: "+ username)
                );

    }
}
