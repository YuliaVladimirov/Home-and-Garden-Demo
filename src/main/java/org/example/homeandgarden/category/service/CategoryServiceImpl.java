package org.example.homeandgarden.category.service;

import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.category.dto.CategoryRequest;
import org.example.homeandgarden.category.dto.CategoryResponse;
import org.example.homeandgarden.category.entity.Category;
import org.example.homeandgarden.category.entity.enums.CategoryStatus;
import org.example.homeandgarden.category.mapper.CategoryMapper;
import org.example.homeandgarden.category.repository.CategoryRepository;
import org.example.homeandgarden.exception.DataAlreadyExistsException;
import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.shared.MessageResponse;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public Page<CategoryResponse> getAllActiveCategories(Integer size, Integer page, String order, String sortBy) {
        Pageable pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);
        return categoryRepository.findAllByCategoryStatus(CategoryStatus.ACTIVE, pageRequest).map(categoryMapper::categoryToResponse);
    }

    @Override
    public Page<CategoryResponse> getCategoriesByStatus(String categoryStatus, Integer size, Integer page, String order, String sortBy) {
        Pageable pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);
        if(categoryStatus == null) {
            return categoryRepository.findAll(pageRequest).map(categoryMapper::categoryToResponse);
        } else {
            CategoryStatus status = CategoryStatus.valueOf(categoryStatus.toUpperCase());
            return categoryRepository.findAllByCategoryStatus(status, pageRequest).map(categoryMapper::categoryToResponse);
        }
    }

    @Override
    @Transactional
    public CategoryResponse addCategory(CategoryRequest categoryRequest) {

        if (categoryRepository.existsByCategoryName(categoryRequest.getCategoryName())) {
            throw new DataAlreadyExistsException(String.format("Category with name: %s, already exists.", categoryRequest.getCategoryName()));
        }
        Category categoryToAdd = categoryMapper.requestToCategory(categoryRequest);
        Category addedCategory = categoryRepository.saveAndFlush(categoryToAdd);
        return categoryMapper.categoryToResponse(addedCategory);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(String categoryId, CategoryRequest categoryRequest) {

        UUID id = UUID.fromString(categoryId);
        Category existingCategory = categoryRepository.findById(id).orElseThrow(() -> new DataNotFoundException (String.format("Category with id: %s, was not found.", categoryId)));

        if (existingCategory.getCategoryStatus().equals(CategoryStatus.INACTIVE)) {
            throw new IllegalArgumentException(String.format("Category with id: %s, is inactive and can not be updated.", categoryId));
        }

        existingCategory.setCategoryName(categoryRequest.getCategoryName());

        Category updatedCategory = categoryRepository.saveAndFlush(existingCategory);
        return categoryMapper.categoryToResponse(updatedCategory);
    }

    @Override
    @Transactional
    public MessageResponse setCategoryStatus(String categoryId, String categoryStatus) {
        UUID id = UUID.fromString(categoryId);
        Category existingCategory = categoryRepository.findById(id).orElseThrow(() -> new DataNotFoundException (String.format("Category with id: %s, was not found.", categoryId)));

        CategoryStatus status = CategoryStatus.valueOf(categoryStatus.toUpperCase());

        if (existingCategory.getCategoryStatus().equals(status)) {
            throw new IllegalArgumentException(String.format("Category with id: %s, already has status '%s'.", categoryId, categoryStatus.toUpperCase()));
        }

        existingCategory.setCategoryStatus(status);
        Category updatedCategory = categoryRepository.saveAndFlush(existingCategory);

        return MessageResponse.builder()
                .message(String.format("Status '%s' was set for category with id: %s.", updatedCategory.getCategoryStatus().name(), updatedCategory.getCategoryId().toString()))
                .build();
    }
}
