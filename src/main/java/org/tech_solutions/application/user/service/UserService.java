package org.tech_solutions.application.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.tech_solutions.application.shared.exception.EntityNotFoundException;
import org.tech_solutions.application.shared.exception.ExistingEntityException;
import org.tech_solutions.application.user.model.User;
import org.tech_solutions.application.user.repository.UserRepository;

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

    public User findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));
    }

    public User update(Long id, User updatedUser) {
        User current = findById(id);

        if (repository.existsByEmailAndIdNot(updatedUser.getEmail(), id)) {
            throw new ExistingEntityException("Usuário já existente no sistema para esse email");
        }

        current.setName(updatedUser.getName());
        current.setEmail(updatedUser.getEmail());
        current.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        current.setUpdatedAt(LocalDateTime.now());

        return repository.save(current);
    }

    public void delete(Long id) {
        User current = findById(id);
        repository.delete(current);
    }
}
