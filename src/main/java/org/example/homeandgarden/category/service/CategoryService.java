package org.example.homeandgarden.category.service;

import org.example.homeandgarden.category.dto.CategoryRequest;
import org.example.homeandgarden.category.dto.CategoryResponse;
import org.example.homeandgarden.shared.MessageResponse;
import org.springframework.data.web.PagedModel;

public interface CategoryService {

    PagedModel<CategoryResponse> getAllCategories(Integer size, Integer page, String order, String sortBy);
    PagedModel<CategoryResponse> getCategoriesByStatus(String categoryStatus, Integer size, Integer page, String order, String sortBy);
    CategoryResponse addCategory(CategoryRequest categoryRequest);
    CategoryResponse updateCategory(String categoryId, CategoryRequest categoryRequest);
    MessageResponse setCategoryStatus(String categoryId, String categoryStatus);


}
