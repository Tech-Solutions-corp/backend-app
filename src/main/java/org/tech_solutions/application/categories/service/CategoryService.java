package org.tech_solutions.application.categories.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.tech_solutions.application.categories.model.Category;
import org.tech_solutions.application.categories.repository.CategoryRepository;
import org.tech_solutions.application.security.CurrentUserService;
import org.tech_solutions.application.shared.exception.EntityNotFoundException;
import org.tech_solutions.application.user.model.User;
import org.tech_solutions.application.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository, CurrentUserService currentUserService) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public Category create(Category category, Long userId) {
        category.setUser(currentUserService.requireCurrentUser());
        category.setCreatedAt(LocalDateTime.now());
        return categoryRepository.save(category);
    }

    public List<Category> listAll() {
        return categoryRepository.findByUserId(currentUserService.requireCurrentUserId());
    }

    public List<Category> listByUser(Long userId) {
        return categoryRepository.findByUserId(currentUserService.requireCurrentUserId());
    }

    public Category findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria nao encontrada"));
        assertOwnedByCurrentUser(category);
        return category;
    }

    public Category update(Long id, Category updated, Long userId) {
        Category current = findById(id);
        current.setUser(currentUserService.requireCurrentUser());
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

    private void assertOwnedByCurrentUser(Category category) {
        Long currentUserId = currentUserService.requireCurrentUserId();
        if (category.getUser() == null || !currentUserId.equals(category.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Recurso nao pertence ao usuario autenticado");
        }
    }
}


