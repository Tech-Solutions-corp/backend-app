package org.tech_solutions.application.categories.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tech_solutions.application.categories.model.Category;

import java.math.BigInteger;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, BigInteger> {
    List<Category> findByUserId(BigInteger userId);
}

