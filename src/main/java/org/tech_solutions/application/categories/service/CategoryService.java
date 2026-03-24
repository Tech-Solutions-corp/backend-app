package org.tech_solutions.application.categories.service;

import org.springframework.stereotype.Service;
import org.tech_solutions.application.categories.model.Category;
import org.tech_solutions.application.categories.repository.CategoryRepository;
import org.tech_solutions.application.shared.exception.EntityNotFoundException;
import org.tech_solutions.application.user.model.User;
import org.tech_solutions.application.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public Category create(Category category, Long userId) {
        category.setUser(findUser(userId));
        category.setCreatedAt(LocalDateTime.now());
        return categoryRepository.save(category);
    }

    public List<Category> listAll() {
        return categoryRepository.findAll();
    }

    public List<Category> listByUser(Long userId) {
        findUser(userId);
        return categoryRepository.findByUserId(userId);
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria nao encontrada"));
    }

    public Category update(Long id, Category updated, Long userId) {
        Category current = findById(id);
        current.setUser(findUser(userId));
        current.setName(updated.getName());
        current.setType(updated.getType());
        return categoryRepository.save(current);
    }

    public void delete(Long id) {
        categoryRepository.delete(findById(id));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));
    }
}


