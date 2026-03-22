package org.tech_solutions.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.tech_solutions.application.exception.ExistingEntityException;
import org.tech_solutions.application.model.User;
import org.tech_solutions.application.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registrate(User newUser) {
        if(repository.existsByEmail(newUser.getEmail())) {
            throw new ExistingEntityException("Usuário já existente no sistema para esse email");
        }
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        return repository.save(newUser);
    }

    public List<User> listAll() {
        return repository.findAll();
    }
}
