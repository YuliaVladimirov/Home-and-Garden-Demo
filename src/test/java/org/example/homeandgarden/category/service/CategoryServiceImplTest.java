package org.example.homeandgarden.category.service;

import org.example.homeandgarden.category.dto.CategoryRequest;
import org.example.homeandgarden.category.dto.CategoryResponse;
import org.example.homeandgarden.category.entity.Category;
import org.example.homeandgarden.category.entity.enums.CategoryStatus;
import org.example.homeandgarden.category.mapper.CategoryMapper;
import org.example.homeandgarden.category.repository.CategoryRepository;
import org.example.homeandgarden.exception.DataAlreadyExistsException;
import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.shared.MessageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.web.PagedModel;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category createCategory(UUID id, String categoryName, CategoryStatus categoryStatus, Instant createdAt, Instant updatedAt) {
        return Category.builder()
                .categoryId(id)
                .categoryName(categoryName)
                .categoryStatus(categoryStatus)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    private CategoryResponse createCategoryResponse(UUID id, String categoryName, CategoryStatus categoryStatus, Instant createdAt, Instant updatedAt) {
        return CategoryResponse.builder()
                .categoryId(id)
                .categoryName(categoryName)
                .categoryStatus(categoryStatus)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    @Test
    void getAllActiveCategories_shouldReturnPagedActiveCategoriesWhenCategoriesExist() {

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "createdAt";

        Pageable pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);

        Category category1 = createCategory(UUID.randomUUID(), "Category One", CategoryStatus.ACTIVE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));
        Category category2 = createCategory(UUID.randomUUID(), "Category Two", CategoryStatus.ACTIVE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        List<Category> allCategories = List.of(category1, category2);
        Page<Category> categoryPage = new PageImpl<>(allCategories, pageRequest, allCategories.size());
        long expectedTotalPages = (long) Math.ceil((double) allCategories.size() / size);

        CategoryResponse categoryResponse1 = createCategoryResponse(category1.getCategoryId(), category1.getCategoryName(), category1.getCategoryStatus(), category1.getCreatedAt(), category1.getUpdatedAt());
        CategoryResponse categoryResponse2 = createCategoryResponse(category2.getCategoryId(), category2.getCategoryName(), category2.getCategoryStatus(), category2.getCreatedAt(), category2.getUpdatedAt());

        when(categoryRepository.findAllByCategoryStatus(CategoryStatus.ACTIVE, pageRequest)).thenReturn(categoryPage);
        when(categoryMapper.categoryToResponse(category1)).thenReturn(categoryResponse1);
        when(categoryMapper.categoryToResponse(category2)).thenReturn(categoryResponse2);

        PagedModel<CategoryResponse> actualResponse = categoryService.getAllActiveCategories(size, page, order, sortBy);

        verify(categoryRepository, times(1)).findAllByCategoryStatus(CategoryStatus.ACTIVE, pageRequest);
        verify(categoryMapper, times(1)).categoryToResponse(category1);
        verify(categoryMapper, times(1)).categoryToResponse(category2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(allCategories.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals(size, actualResponse.getMetadata().size());
        assertEquals(page, actualResponse.getMetadata().number());

        assertNotNull(actualResponse.getContent());
        assertEquals(allCategories.size(), actualResponse.getContent().size());
        assertEquals(categoryResponse1.getCategoryId(), actualResponse.getContent().getFirst().getCategoryId());
        assertEquals(categoryResponse1.getCategoryName(), actualResponse.getContent().getFirst().getCategoryName());
        assertEquals(CategoryStatus.ACTIVE, actualResponse.getContent().getFirst().getCategoryStatus());
        assertEquals(categoryResponse2.getCategoryId(), actualResponse.getContent().get(1).getCategoryId());
        assertEquals(categoryResponse2.getCategoryName(), actualResponse.getContent().get(1).getCategoryName());
        assertEquals(CategoryStatus.ACTIVE, actualResponse.getContent().get(1).getCategoryStatus());
    }

    @Test
    void getAllActiveCategories_shouldReturnEmptyPagedModelWhenNoActiveCategoriesExist() {

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "createdAt";

        Pageable pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);
        Page<Category> emptyPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(categoryRepository.findAllByCategoryStatus(CategoryStatus.ACTIVE, pageRequest)).thenReturn(emptyPage);

        PagedModel<CategoryResponse> actualResponse = categoryService.getAllActiveCategories(size, page, order, sortBy);

        verify(categoryRepository, times(1)).findAllByCategoryStatus(CategoryStatus.ACTIVE, pageRequest);
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(0L, actualResponse.getMetadata().totalElements());
        assertEquals(0L, actualResponse.getMetadata().totalPages());
        assertEquals(size, actualResponse.getMetadata().size());
        assertEquals(page, actualResponse.getMetadata().number());

        assertNotNull(actualResponse.getContent());
        assertTrue(actualResponse.getContent().isEmpty());
        assertEquals(0, actualResponse.getContent().size());
    }

    @Test
    void getCategoriesByStatus_shouldRetrieveAndMapCategoriesByCertainStatusWithPagination() {

        CategoryStatus status = CategoryStatus.INACTIVE;
        String categoryStatus = status.name();

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "createdAt";

        Pageable pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);

        Category category1 = createCategory(UUID.randomUUID(), "Category One", status, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));
        Category category2 = createCategory(UUID.randomUUID(), "Category Two", status, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        List<Category> allCategories = List.of(category1, category2);
        Page<Category> categoryPage = new PageImpl<>(allCategories, pageRequest, allCategories.size());
        long expectedTotalPages = (long) Math.ceil((double) allCategories.size() / size);

        CategoryResponse categoryResponse1 = createCategoryResponse(category1.getCategoryId(), category1.getCategoryName(), category1.getCategoryStatus(), category1.getCreatedAt(), category1.getUpdatedAt());
        CategoryResponse categoryResponse2 = createCategoryResponse(category2.getCategoryId(), category2.getCategoryName(), category2.getCategoryStatus(), category2.getCreatedAt(), category2.getUpdatedAt());

        when(categoryRepository.findAllByCategoryStatus(status, pageRequest)).thenReturn(categoryPage);
        when(categoryMapper.categoryToResponse(category1)).thenReturn(categoryResponse1);
        when(categoryMapper.categoryToResponse(category2)).thenReturn(categoryResponse2);

        PagedModel<CategoryResponse> actualResponse = categoryService.getCategoriesByStatus(categoryStatus, size, page, order, sortBy);

        verify(categoryRepository, times(1)).findAllByCategoryStatus(status, pageRequest);
        verify(categoryMapper, times(1)).categoryToResponse(category1);
        verify(categoryMapper, times(1)).categoryToResponse(category2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(allCategories.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals(size, actualResponse.getMetadata().size());
        assertEquals(page, actualResponse.getMetadata().number());

        assertNotNull(actualResponse.getContent());
        assertEquals(allCategories.size(), actualResponse.getContent().size());
        assertEquals(categoryResponse1.getCategoryId(), actualResponse.getContent().getFirst().getCategoryId());
        assertEquals(categoryResponse1.getCategoryName(), actualResponse.getContent().getFirst().getCategoryName());
        assertEquals(categoryResponse1.getCategoryStatus(), actualResponse.getContent().getFirst().getCategoryStatus());
        assertEquals(categoryResponse2.getCategoryId(), actualResponse.getContent().get(1).getCategoryId());
        assertEquals(categoryResponse2.getCategoryName(), actualResponse.getContent().get(1).getCategoryName());
        assertEquals(categoryResponse2.getCategoryStatus(), actualResponse.getContent().get(1).getCategoryStatus());
    }

    @Test
    void getCategoriesByStatus_shouldRetrieveAndMapAllCategoriesWithPaginationIfNoStatusIsProvided() {

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "createdAt";

        Pageable pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);

        Category category1 = createCategory(UUID.randomUUID(), "Category One", CategoryStatus.INACTIVE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));
        Category category2 = createCategory(UUID.randomUUID(), "Category Two", CategoryStatus.INACTIVE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        List<Category> allCategories = List.of(category1, category2);
        Page<Category> categoryPage = new PageImpl<>(allCategories, pageRequest, allCategories.size());
        long expectedTotalPages = (long) Math.ceil((double) allCategories.size() / size);

        CategoryResponse categoryResponse1 = createCategoryResponse(category1.getCategoryId(), category1.getCategoryName(), category1.getCategoryStatus(), category1.getCreatedAt(), category1.getUpdatedAt());
        CategoryResponse categoryResponse2 = createCategoryResponse(category2.getCategoryId(), category2.getCategoryName(), category2.getCategoryStatus(), category2.getCreatedAt(), category2.getUpdatedAt());

        when(categoryRepository.findAll(pageRequest)).thenReturn(categoryPage);
        when(categoryMapper.categoryToResponse(category1)).thenReturn(categoryResponse1);
        when(categoryMapper.categoryToResponse(category2)).thenReturn(categoryResponse2);

        PagedModel<CategoryResponse> actualResponse = categoryService.getCategoriesByStatus(null, size, page, order, sortBy);

        verify(categoryRepository, times(1)).findAll(pageRequest);
        verify(categoryMapper, times(1)).categoryToResponse(category1);
        verify(categoryMapper, times(1)).categoryToResponse(category2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(allCategories.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals(size, actualResponse.getMetadata().size());
        assertEquals(page, actualResponse.getMetadata().number());

        assertNotNull(actualResponse.getContent());
        assertEquals(allCategories.size(), actualResponse.getContent().size());
        assertEquals(categoryResponse1.getCategoryId(), actualResponse.getContent().getFirst().getCategoryId());
        assertEquals(categoryResponse1.getCategoryName(), actualResponse.getContent().getFirst().getCategoryName());
        assertEquals(categoryResponse1.getCategoryStatus(), actualResponse.getContent().getFirst().getCategoryStatus());
        assertEquals(categoryResponse2.getCategoryId(), actualResponse.getContent().get(1).getCategoryId());
        assertEquals(categoryResponse2.getCategoryName(), actualResponse.getContent().get(1).getCategoryName());
        assertEquals(categoryResponse2.getCategoryStatus(), actualResponse.getContent().get(1).getCategoryStatus());
    }

    @Test
    void getCategoriesByStatus_shouldThrowIllegalArgumentExceptionWhenCategoryStatusIsInvalid (){

        String invalidStatus = "INVALID_STATUS";

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "createdAt";

        assertThrows(IllegalArgumentException.class, () ->
                categoryService.getCategoriesByStatus(invalidStatus, size, page, order, sortBy));

        verify(categoryRepository, never()).findAllByCategoryStatus(any(CategoryStatus.class), any(Pageable.class));
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));
    }


    @Test
    void addCategory_shouldAddCategorySuccessfullyWhenCategoryNameDoesNotExist() {

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .categoryName("New Category")
                .build();

        Category categoryToSave = createCategory(null, categoryRequest.getCategoryName(), CategoryStatus.ACTIVE, Instant.now(), Instant.now());
        Category savedCategory = createCategory(UUID.randomUUID(), categoryToSave.getCategoryName(), categoryToSave.getCategoryStatus(), Instant.now(), Instant.now());

        CategoryResponse categoryResponse = createCategoryResponse(savedCategory.getCategoryId(), savedCategory.getCategoryName(), savedCategory.getCategoryStatus(), savedCategory.getCreatedAt(), savedCategory.getUpdatedAt());

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);

        when(categoryRepository.existsByCategoryName(categoryRequest.getCategoryName())).thenReturn(false);
        when(categoryMapper.requestToCategory(categoryRequest)).thenReturn(categoryToSave);
        when(categoryRepository.saveAndFlush(categoryCaptor.capture())).thenReturn(savedCategory);
        when(categoryMapper.categoryToResponse(savedCategory)).thenReturn(categoryResponse);

        CategoryResponse actualResponse = categoryService.addCategory(categoryRequest);

        verify(categoryRepository, times(1)).existsByCategoryName(categoryRequest.getCategoryName());
        verify(categoryMapper, times(1)).requestToCategory(categoryRequest);

        verify(categoryRepository, times(1)).saveAndFlush(categoryToSave);
        Category capturedCategory = categoryCaptor.getValue();
        assertNotNull(capturedCategory);
        assertEquals(categoryRequest.getCategoryName(), capturedCategory.getCategoryName());
        assertEquals(CategoryStatus.ACTIVE, capturedCategory.getCategoryStatus());

        verify(categoryMapper, times(1)).categoryToResponse(savedCategory);
        assertNotNull(actualResponse);
        assertEquals(categoryResponse.getCategoryId(), actualResponse.getCategoryId());
        assertEquals(categoryResponse.getCategoryName(), actualResponse.getCategoryName());
        assertEquals(categoryResponse.getCategoryStatus(), actualResponse.getCategoryStatus());
        assertEquals(categoryResponse.getCreatedAt(), actualResponse.getCreatedAt());
        assertEquals(categoryResponse.getUpdatedAt(), actualResponse.getUpdatedAt());
    }

    @Test
    void addCategory_shouldThrowDataAlreadyExistsExceptionWhenCategoryNameAlreadyExists() {

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .categoryName("Existing Category")
                .build();

        when(categoryRepository.existsByCategoryName(categoryRequest.getCategoryName())).thenReturn(true);

        DataAlreadyExistsException thrownException = assertThrows(DataAlreadyExistsException.class, () ->
                categoryService.addCategory(categoryRequest));

        verify(categoryRepository, times(1)).existsByCategoryName(categoryRequest.getCategoryName());
        verify(categoryMapper, never()).requestToCategory(any(CategoryRequest.class));
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));

        assertEquals(String.format("Category with name: %s, already exists.", categoryRequest.getCategoryName()), thrownException.getMessage());
    }


    @Test
    void updateCategory_shouldUpdateCategorySuccessfullyWhenCategoryExistsAndIsActive() {

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .categoryName("Updated Name")
                .build();

        Category existingCategory = createCategory(id, "Original Name", CategoryStatus.ACTIVE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));
        Category updatedCategory = createCategory(existingCategory.getCategoryId(), categoryRequest.getCategoryName(), existingCategory.getCategoryStatus(), existingCategory.getCreatedAt(), Instant.now());

        CategoryResponse categoryResponse = createCategoryResponse(updatedCategory.getCategoryId(), updatedCategory.getCategoryName(), updatedCategory.getCategoryStatus(), updatedCategory.getCreatedAt(), updatedCategory.getUpdatedAt());

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.saveAndFlush(categoryCaptor.capture())).thenReturn(updatedCategory);
        when(categoryMapper.categoryToResponse(updatedCategory)).thenReturn(categoryResponse);

        CategoryResponse actualResponse = categoryService.updateCategory(categoryId, categoryRequest);

        verify(categoryRepository, times(1)).findById(id);
        verify(categoryRepository, times(1)).saveAndFlush(existingCategory);
        Category capturedCategory = categoryCaptor.getValue();
        assertNotNull(capturedCategory);
        assertEquals(categoryRequest.getCategoryName(), capturedCategory.getCategoryName());
        assertEquals(CategoryStatus.ACTIVE, capturedCategory.getCategoryStatus());

        verify(categoryMapper, times(1)).categoryToResponse(updatedCategory);

        assertNotNull(actualResponse);
        assertEquals(categoryResponse.getCategoryId(), actualResponse.getCategoryId());
        assertEquals(categoryRequest.getCategoryName(), actualResponse.getCategoryName());
        assertEquals(categoryResponse.getCategoryStatus(), actualResponse.getCategoryStatus());
        assertEquals(categoryResponse.getCreatedAt(), actualResponse.getCreatedAt());
        assertTrue(actualResponse.getUpdatedAt().isAfter(existingCategory.getUpdatedAt()));
    }

    @Test
    void updateCategory_shouldNotChangeCategoryWhenCategoryExistsAndIsActiveButNameIsNullInRequest() {

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .categoryName(null)
                .build();

        Category existingCategory = createCategory(id, "Original Name", CategoryStatus.ACTIVE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));
        Category updatedCategory = createCategory(existingCategory.getCategoryId(), existingCategory.getCategoryName(), existingCategory.getCategoryStatus(), existingCategory.getCreatedAt(), Instant.now());

        CategoryResponse categoryResponse = createCategoryResponse(updatedCategory.getCategoryId(), updatedCategory.getCategoryName(), updatedCategory.getCategoryStatus(), updatedCategory.getCreatedAt(), updatedCategory.getUpdatedAt());

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.saveAndFlush(categoryCaptor.capture())).thenReturn(updatedCategory);
        when(categoryMapper.categoryToResponse(updatedCategory)).thenReturn(categoryResponse);

        CategoryResponse actualResponse = categoryService.updateCategory(categoryId, categoryRequest);

        verify(categoryRepository, times(1)).findById(id);

        verify(categoryRepository, times(1)).saveAndFlush(existingCategory);
        Category capturedCategory = categoryCaptor.getValue();
        assertNotNull(capturedCategory);
        assertEquals(existingCategory.getCategoryName(), capturedCategory.getCategoryName());
        assertEquals(CategoryStatus.ACTIVE, capturedCategory.getCategoryStatus());

        verify(categoryMapper, times(1)).categoryToResponse(updatedCategory);

        assertNotNull(actualResponse);
        assertEquals(categoryResponse.getCategoryId(), actualResponse.getCategoryId());
        assertEquals(categoryResponse.getCategoryName(), actualResponse.getCategoryName());
        assertEquals(categoryResponse.getCategoryStatus(), actualResponse.getCategoryStatus());
        assertEquals(categoryResponse.getCreatedAt(), actualResponse.getCreatedAt());
        assertTrue(actualResponse.getUpdatedAt().isAfter(existingCategory.getUpdatedAt()));
    }

    @Test
    void updateCategory_shouldThrowDataNotFoundExceptionWhenCategoryDoesNotExist() {

        UUID nonExistingId = UUID.randomUUID();
        String nonExistingCategoryId = nonExistingId.toString();

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .categoryName("Updated Name")
                .build();

        when(categoryRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                categoryService.updateCategory(nonExistingCategoryId, categoryRequest));

        verify(categoryRepository, times(1)).findById(nonExistingId);
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));

        assertEquals(String.format("Category with id: %s, was not found.", nonExistingCategoryId), thrownException.getMessage());
    }

    @Test
    void updateCategory_shouldThrowIllegalArgumentExceptionWhenCategoryIsInactive() {

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .categoryName("Updated Name")
                .build();

        Category existingCategory = createCategory(id, "Original Name", CategoryStatus.INACTIVE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
                categoryService.updateCategory(categoryId, categoryRequest));

        verify(categoryRepository, times(1)).findById(id);
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));

        assertEquals(String.format("Category with id: %s, is inactive and can not be updated.", categoryId), thrownException.getMessage());
    }

    @Test
    void updateCategory_shouldThrowIllegalArgumentExceptionWhenCategoryIdIsInvalidUuidString() {

        String invalidCategoryId = "INVALID_UUID";

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .categoryName("Updated Name")
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                categoryService.updateCategory(invalidCategoryId, categoryRequest));

        verify(categoryRepository, never()).findById(any(UUID.class));
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));
    }

    @Test
    void setCategoryStatus_shouldSetCategoryStatusSuccessfullyWhenCategoryExistsAndStatusIsDifferent() {

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        CategoryStatus newStatus = CategoryStatus.INACTIVE;
        String categoryNewStatus = newStatus.name();

        Category existingCategory = createCategory(id, "Existing Category", CategoryStatus.ACTIVE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));
        Category updatedCategory = createCategory(existingCategory.getCategoryId(), existingCategory.getCategoryName(), newStatus, existingCategory.getCreatedAt(), Instant.now());

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("Status '%s' was set for category with id: %s.", categoryNewStatus, categoryId))
                .build();

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.saveAndFlush(categoryCaptor.capture())).thenReturn(updatedCategory);

        MessageResponse actualResponse = categoryService.setCategoryStatus(categoryId, categoryNewStatus);

        verify(categoryRepository, times(1)).findById(id);
        verify(categoryRepository, times(1)).saveAndFlush(existingCategory);
        Category capturedCategory = categoryCaptor.getValue();
        assertNotNull(capturedCategory);
        assertEquals(existingCategory.getCategoryName(), capturedCategory.getCategoryName());
        assertEquals(newStatus, capturedCategory.getCategoryStatus());

        assertNotNull(actualResponse);
        assertEquals(messageResponse.getMessage(), actualResponse.getMessage());
    }

    @Test
    void setCategoryStatus_shouldThrowDataNotFoundExceptionWhenCategoryDoesNotExist() {

        UUID nonExistingId = UUID.randomUUID();
        String nonExistingCategoryId = nonExistingId.toString();

        String newStatus = CategoryStatus.INACTIVE.name();

        when(categoryRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                categoryService.setCategoryStatus(nonExistingCategoryId, newStatus));

        verify(categoryRepository, times(1)).findById(nonExistingId);
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));

        assertEquals(String.format("Category with id: %s, was not found.", nonExistingCategoryId), thrownException.getMessage());
    }

    @Test
    void setCategoryStatus_shouldThrowIllegalArgumentExceptionWhenCategoryAlreadyHasTargetStatus() {

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        String newStatus = CategoryStatus.ACTIVE.name();

        Category existingCategory = createCategory(id, "Existing Category", CategoryStatus.ACTIVE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
                categoryService.setCategoryStatus(categoryId, newStatus));

        verify(categoryRepository, times(1)).findById(id);
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));

        assertEquals(String.format("Category with id: %s, already has status '%s'.", categoryId, newStatus.toUpperCase()), thrownException.getMessage());
    }

    @Test
    void setCategoryStatus_shouldThrowIllegalArgumentExceptionWhenInvalidUuidStringIsProvided() {

        String invalidCategoryId = "INVALID_UUID";
        String newStatus = CategoryStatus.INACTIVE.name();

        assertThrows(IllegalArgumentException.class, () ->
                categoryService.setCategoryStatus(invalidCategoryId, newStatus));

        verify(categoryRepository, never()).findById(any(UUID.class));
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
    }

    @Test
    void setCategoryStatus_shouldThrowIllegalArgumentExceptionWhenInvalidStatusStringIsProvided() {

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        String invalidStatus = "INVALID_STATUS";

        Category existingCategory = createCategory(id, "Existing Category", CategoryStatus.ACTIVE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));

        assertThrows(IllegalArgumentException.class, () ->
                categoryService.setCategoryStatus(categoryId, invalidStatus));

        verify(categoryRepository, times(1)).findById(id);
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
    }

    @Test
    void setCategoryStatus_shouldThrowIllegalStateExceptionWhenStatusUpdateFailsOnSave() {

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        CategoryStatus newStatus = CategoryStatus.INACTIVE;
        String categoryNewStatus = newStatus.name();

        Category existingCategory = createCategory(id, "Existing Category", CategoryStatus.ACTIVE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        Category categoryWithOriginalStatus = createCategory(id, "Existing Category", CategoryStatus.ACTIVE, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS));

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.saveAndFlush(categoryCaptor.capture())).thenReturn(categoryWithOriginalStatus);

        IllegalStateException thrownException = assertThrows(IllegalStateException.class, () ->
                categoryService.setCategoryStatus(categoryId, categoryNewStatus));

        verify(categoryRepository, times(1)).findById(id);
        verify(categoryRepository, times(1)).saveAndFlush(existingCategory);
        Category capturedCategory = categoryCaptor.getValue();
        assertNotNull(capturedCategory);
        assertEquals(existingCategory.getCategoryId(), capturedCategory.getCategoryId());
        assertEquals(existingCategory.getCategoryName(), capturedCategory.getCategoryName());
        assertEquals(newStatus, capturedCategory.getCategoryStatus());

        assertEquals(String.format("Unfortunately something went wrong and status '%s' was not set for category with id: %s. Please, try again.", categoryNewStatus, categoryId), thrownException.getMessage());
    }
}