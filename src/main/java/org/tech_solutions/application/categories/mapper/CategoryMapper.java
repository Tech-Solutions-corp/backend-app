package org.tech_solutions.application.categories.mapper;

import org.tech_solutions.application.categories.dto.CategoryDataDTO;
import org.tech_solutions.application.categories.dto.CategoryRequestDTO;
import org.tech_solutions.application.categories.model.Category;

import java.util.List;

public class CategoryMapper {

    private CategoryMapper() {
    }

    public static Category toModel(CategoryRequestDTO dto) {
        Category category = new Category();
        category.setName(dto.name());
        category.setType(dto.type());
        return category;
    }

    public static CategoryDataDTO toDTO(Category category) {
        return new CategoryDataDTO(
                category.getId(),
                category.getUser().getId(),
                category.getName(),
                category.getType(),
                category.getCreatedAt()
        );
    }

    public static List<CategoryDataDTO> toDTO(List<Category> categories) {
        return categories.stream().map(CategoryMapper::toDTO).toList();
    }
}

