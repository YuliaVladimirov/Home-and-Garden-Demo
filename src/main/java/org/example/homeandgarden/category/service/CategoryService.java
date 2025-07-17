package org.example.homeandgarden.category.service;

import org.example.homeandgarden.category.dto.CategoryRequest;
import org.example.homeandgarden.category.dto.CategoryResponse;
import org.example.homeandgarden.shared.MessageResponse;
import org.springframework.data.domain.Page;

public interface CategoryService {

    Page<CategoryResponse> getAllActiveCategories(Integer size, Integer page, String order, String sortBy);
    Page<CategoryResponse> getCategoriesByStatus(String categoryStatus, Integer size, Integer page, String order, String sortBy);
    CategoryResponse addCategory(CategoryRequest categoryRequest);
    CategoryResponse updateCategory(String categoryId, CategoryRequest categoryRequest);
    MessageResponse setCategoryStatus(String categoryId, String categoryStatus);


}
