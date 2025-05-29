package org.example.homeandgarden.category.mapper;

import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.category.dto.CategoryRequest;
import org.example.homeandgarden.category.dto.CategoryResponse;
import org.example.homeandgarden.category.entity.Category;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryMapper {

    public Category requestToCategory(
            CategoryRequest categoryRequest) {

        return Category.builder()
                .categoryName(categoryRequest.getCategoryName())
                .build();
    }

    public CategoryResponse categoryToResponse(Category category) {

        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .categoryStatus(category.getCategoryStatus())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
