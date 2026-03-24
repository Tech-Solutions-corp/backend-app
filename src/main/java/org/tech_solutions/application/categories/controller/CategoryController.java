package org.tech_solutions.application.categories.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tech_solutions.application.categories.dto.CategoryDataDTO;
import org.tech_solutions.application.categories.dto.CategoryRequestDTO;
import org.tech_solutions.application.categories.mapper.CategoryMapper;
import org.tech_solutions.application.categories.model.Category;
import org.tech_solutions.application.categories.service.CategoryService;

import java.util.List;

@RestController
@RequestMapping("api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryDataDTO> create(@Valid @RequestBody CategoryRequestDTO request) {
        Category created = categoryService.create(CategoryMapper.toModel(request), request.userId());
        return ResponseEntity.status(201).body(CategoryMapper.toDTO(created));
    }

    @GetMapping
    public ResponseEntity<List<CategoryDataDTO>> listAll() {
        List<Category> categories = categoryService.listAll();
        return categories.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(CategoryMapper.toDTO(categories));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CategoryDataDTO>> listByUser(@PathVariable Long userId) {
        List<Category> categories = categoryService.listByUser(userId);
        return categories.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(CategoryMapper.toDTO(categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDataDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(CategoryMapper.toDTO(categoryService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDataDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDTO request
    ) {
        Category updated = categoryService.update(id, CategoryMapper.toModel(request), request.userId());
        return ResponseEntity.ok(CategoryMapper.toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}


