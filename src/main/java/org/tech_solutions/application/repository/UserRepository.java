package org.tech_solutions.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tech_solutions.application.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

}
