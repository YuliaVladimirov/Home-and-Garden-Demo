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

import java.time.Instant;
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

    private static final Integer PAGE = 0;
    private static final Integer SIZE = 5;
    private static final String ORDER = "ASC";
    private static final String SORT_BY = "createdAt";

    private static final UUID CATEGORY_1_ID = UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6");
    private static final UUID CATEGORY_2_ID = UUID.fromString("e9b1d3b0-146a-4be0-a1e2-2b9e18b1a8cf");

    private static final UUID CATEGORY_ID = UUID.fromString("aebdd1a2-1bc2-4cc9-8496-674e4c7ee5f2");
    private static final UUID NON_EXISTING_CATEGORY_ID = UUID.fromString("de305d54-75b4-431b-adb2-eb6b9e546014");

    private static final String INVALID_ID = "Invalid UUID";

    private static final CategoryStatus CATEGORY_STATUS_ACTIVE = CategoryStatus.ACTIVE;
    private static final CategoryStatus CATEGORY_STATUS_INACTIVE = CategoryStatus.INACTIVE;

    private static final String INVALID_STATUS = "Invalid Status";

    private final Instant TIMESTAMP_NOW = Instant.now();
    private static final Instant TIMESTAMP_PAST = Instant.parse("2024-12-01T12:00:00Z");

    @Test
    void getAllActiveCategories_shouldReturnPagedActiveCategoriesWhenCategoriesExist() {

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        Category category1 = Category.builder()
                .categoryId(CATEGORY_1_ID)
                .categoryName("Category One")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        Category category2 = Category.builder()
                .categoryId(CATEGORY_2_ID)
                .categoryName("Category Two")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        List<Category> allCategories = List.of(category1, category2);
        Page<Category> categoryPage = new PageImpl<>(allCategories, pageRequest, allCategories.size());
        long expectedTotalPages = (long) Math.ceil((double) allCategories.size() / SIZE);

        CategoryResponse categoryResponse1 = CategoryResponse.builder()
                .categoryId(category1.getCategoryId())
                .categoryName(category1.getCategoryName())
                .categoryStatus(category1.getCategoryStatus())
                .createdAt(category1.getCreatedAt())
                .updatedAt(category1.getUpdatedAt())
                .build();

        CategoryResponse categoryResponse2 = CategoryResponse.builder()
                .categoryId(category2.getCategoryId())
                .categoryName(category2.getCategoryName())
                .categoryStatus(category2.getCategoryStatus())
                .createdAt(category2.getCreatedAt())
                .updatedAt(category2.getUpdatedAt())
                .build();

        when(categoryRepository.findAllByCategoryStatus(CATEGORY_STATUS_ACTIVE, pageRequest)).thenReturn(categoryPage);
        when(categoryMapper.categoryToResponse(category1)).thenReturn(categoryResponse1);
        when(categoryMapper.categoryToResponse(category2)).thenReturn(categoryResponse2);

        Page<CategoryResponse> actualResponse = categoryService.getAllActiveCategories(SIZE, PAGE, ORDER, SORT_BY);

        verify(categoryRepository, times(1)).findAllByCategoryStatus(CATEGORY_STATUS_ACTIVE, pageRequest);
        verify(categoryMapper, times(1)).categoryToResponse(category1);
        verify(categoryMapper, times(1)).categoryToResponse(category2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(allCategories.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long)SIZE, actualResponse.getSize());
        assertEquals((long)PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(allCategories.size(), actualResponse.getContent().size());
        assertEquals(categoryResponse1.getCategoryId(), actualResponse.getContent().getFirst().getCategoryId());
        assertEquals(categoryResponse1.getCategoryName(), actualResponse.getContent().getFirst().getCategoryName());
        assertEquals(CATEGORY_STATUS_ACTIVE, actualResponse.getContent().getFirst().getCategoryStatus());
        assertEquals(categoryResponse2.getCategoryId(), actualResponse.getContent().get(1).getCategoryId());
        assertEquals(categoryResponse2.getCategoryName(), actualResponse.getContent().get(1).getCategoryName());
        assertEquals(CATEGORY_STATUS_ACTIVE, actualResponse.getContent().get(1).getCategoryStatus());
    }

    @Test
    void getAllActiveCategories_shouldReturnEmptyPagedModelWhenNoActiveCategoriesExist() {

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);
        Page<Category> emptyPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(categoryRepository.findAllByCategoryStatus(CATEGORY_STATUS_ACTIVE, pageRequest)).thenReturn(emptyPage);

        Page<CategoryResponse> actualResponse = categoryService.getAllActiveCategories(SIZE, PAGE, ORDER, SORT_BY);

        verify(categoryRepository, times(1)).findAllByCategoryStatus(CATEGORY_STATUS_ACTIVE, pageRequest);
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(0L, actualResponse.getTotalElements());
        assertEquals(0L, actualResponse.getTotalPages());
        assertEquals((long)SIZE, actualResponse.getSize());
        assertEquals((long)PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertTrue(actualResponse.getContent().isEmpty());
        assertEquals(0, actualResponse.getContent().size());
    }

    @Test
    void getCategoriesByStatus_shouldRetrieveAndMapCategoriesByCertainStatusWithPagination() {

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        Category category1 = Category.builder()
                .categoryId(CATEGORY_1_ID)
                .categoryName("Category One")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        Category category2 = Category.builder()
                .categoryId(CATEGORY_2_ID)
                .categoryName("Category Two")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        List<Category> allCategories = List.of(category1, category2);
        Page<Category> categoryPage = new PageImpl<>(allCategories, pageRequest, allCategories.size());
        long expectedTotalPages = (long) Math.ceil((double) allCategories.size() / SIZE);

        CategoryResponse categoryResponse1 = CategoryResponse.builder()
                .categoryId(category1.getCategoryId())
                .categoryName(category1.getCategoryName())
                .categoryStatus(category1.getCategoryStatus())
                .createdAt(category1.getCreatedAt())
                .updatedAt(category1.getUpdatedAt())
                .build();

        CategoryResponse categoryResponse2 = CategoryResponse.builder()
                .categoryId(category2.getCategoryId())
                .categoryName(category2.getCategoryName())
                .categoryStatus(category2.getCategoryStatus())
                .createdAt(category2.getCreatedAt())
                .updatedAt(category2.getUpdatedAt())
                .build();

        when(categoryRepository.findAllByCategoryStatus(CATEGORY_STATUS_ACTIVE, pageRequest)).thenReturn(categoryPage);
        when(categoryMapper.categoryToResponse(category1)).thenReturn(categoryResponse1);
        when(categoryMapper.categoryToResponse(category2)).thenReturn(categoryResponse2);

        Page<CategoryResponse> actualResponse = categoryService.getCategoriesByStatus(CATEGORY_STATUS_ACTIVE.name(), SIZE, PAGE, ORDER, SORT_BY);

        verify(categoryRepository, times(1)).findAllByCategoryStatus(CATEGORY_STATUS_ACTIVE, pageRequest);
        verify(categoryMapper, times(1)).categoryToResponse(category1);
        verify(categoryMapper, times(1)).categoryToResponse(category2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(allCategories.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long)SIZE, actualResponse.getSize());
        assertEquals((long)PAGE, actualResponse.getNumber());

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

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        Category category1 = Category.builder()
                .categoryId(CATEGORY_1_ID)
                .categoryName("Category One")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        Category category2 = Category.builder()
                .categoryId(CATEGORY_2_ID)
                .categoryName("Category Two")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        List<Category> allCategories = List.of(category1, category2);
        Page<Category> categoryPage = new PageImpl<>(allCategories, pageRequest, allCategories.size());
        long expectedTotalPages = (long) Math.ceil((double) allCategories.size() / SIZE);

        CategoryResponse categoryResponse1 = CategoryResponse.builder()
                .categoryId(category1.getCategoryId())
                .categoryName(category1.getCategoryName())
                .categoryStatus(category1.getCategoryStatus())
                .createdAt(category1.getCreatedAt())
                .updatedAt(category1.getUpdatedAt())
                .build();

        CategoryResponse categoryResponse2 = CategoryResponse.builder()
                .categoryId(category2.getCategoryId())
                .categoryName(category2.getCategoryName())
                .categoryStatus(category2.getCategoryStatus())
                .createdAt(category2.getCreatedAt())
                .updatedAt(category2.getUpdatedAt())
                .build();

        when(categoryRepository.findAll(pageRequest)).thenReturn(categoryPage);
        when(categoryMapper.categoryToResponse(category1)).thenReturn(categoryResponse1);
        when(categoryMapper.categoryToResponse(category2)).thenReturn(categoryResponse2);

        Page<CategoryResponse> actualResponse = categoryService.getCategoriesByStatus(null, SIZE, PAGE, ORDER, SORT_BY);

        verify(categoryRepository, times(1)).findAll(pageRequest);
        verify(categoryMapper, times(1)).categoryToResponse(category1);
        verify(categoryMapper, times(1)).categoryToResponse(category2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(allCategories.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long)SIZE, actualResponse.getSize());
        assertEquals((long)PAGE, actualResponse.getNumber());

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

        assertThrows(IllegalArgumentException.class, () ->
                categoryService.getCategoriesByStatus(INVALID_STATUS, SIZE, PAGE, ORDER, SORT_BY));

        verify(categoryRepository, never()).findAllByCategoryStatus(any(CategoryStatus.class), any(Pageable.class));
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));
    }

    @Test
    void addCategory_shouldAddCategorySuccessfullyWhenCategoryNameDoesNotExist() {

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .categoryName("New Category")
                .build();

        Category categoryToSave = Category.builder()
                .categoryId(null)
                .categoryName(categoryRequest.getCategoryName())
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(null)
                .updatedAt(null)
                .build();

        Category savedCategory = Category.builder()
                .categoryId(CATEGORY_ID)
                .categoryName(categoryToSave.getCategoryName())
                .categoryStatus(categoryToSave.getCategoryStatus())
                .createdAt(TIMESTAMP_NOW)
                .updatedAt(TIMESTAMP_NOW)
                .build();

        CategoryResponse categoryResponse = CategoryResponse.builder()
                .categoryId(savedCategory.getCategoryId())
                .categoryName(savedCategory.getCategoryName())
                .categoryStatus(savedCategory.getCategoryStatus())
                .createdAt(savedCategory.getCreatedAt())
                .updatedAt(savedCategory.getUpdatedAt())
                .build();

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);

        when(categoryRepository.existsByCategoryName(categoryRequest.getCategoryName())).thenReturn(false);
        when(categoryMapper.requestToCategory(categoryRequest)).thenReturn(categoryToSave);
        when(categoryRepository.save(categoryToSave)).thenReturn(savedCategory);
        when(categoryMapper.categoryToResponse(savedCategory)).thenReturn(categoryResponse);

        CategoryResponse actualResponse = categoryService.addCategory(categoryRequest);

        verify(categoryRepository, times(1)).existsByCategoryName(categoryRequest.getCategoryName());
        verify(categoryMapper, times(1)).requestToCategory(categoryRequest);

        verify(categoryRepository, times(1)).save(categoryCaptor.capture());
        Category capturedCategory = categoryCaptor.getValue();
        assertNotNull(capturedCategory);
        assertEquals(categoryRequest.getCategoryName(), capturedCategory.getCategoryName());
        assertEquals(CATEGORY_STATUS_ACTIVE, capturedCategory.getCategoryStatus());

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
        verify(categoryRepository, never()).save(any(Category.class));
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));

        assertEquals(String.format("Category with name: %s, already exists.", categoryRequest.getCategoryName()), thrownException.getMessage());
    }


    @Test
    void updateCategory_shouldUpdateCategorySuccessfullyWhenCategoryExistsAndIsActive() {

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .categoryName("Updated Name")
                .build();

        Category existingCategory = Category.builder()
                .categoryId(CATEGORY_ID)
                .categoryName("Original Name")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        Category updatedCategory = Category.builder()
                .categoryId(existingCategory.getCategoryId())
                .categoryName(categoryRequest.getCategoryName())
                .categoryStatus(existingCategory.getCategoryStatus())
                .createdAt(existingCategory.getCreatedAt())
                .updatedAt(TIMESTAMP_NOW)
                .build();

        CategoryResponse categoryResponse = CategoryResponse.builder()
                .categoryId(updatedCategory.getCategoryId())
                .categoryName(updatedCategory.getCategoryName())
                .categoryStatus(updatedCategory.getCategoryStatus())
                .createdAt(updatedCategory.getCreatedAt())
                .updatedAt(updatedCategory.getUpdatedAt())
                .build();

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(existingCategory)).thenReturn(updatedCategory);
        when(categoryMapper.categoryToResponse(updatedCategory)).thenReturn(categoryResponse);

        CategoryResponse actualResponse = categoryService.updateCategory(CATEGORY_ID.toString(), categoryRequest);

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(categoryRepository, times(1)).save(categoryCaptor.capture());
        Category capturedCategory = categoryCaptor.getValue();
        assertNotNull(capturedCategory);
        assertEquals(categoryRequest.getCategoryName(), capturedCategory.getCategoryName());
        assertEquals(CATEGORY_STATUS_ACTIVE, capturedCategory.getCategoryStatus());

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

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .categoryName(null)
                .build();

        Category existingCategory = Category.builder()
                .categoryId(CATEGORY_ID)
                .categoryName("Original Name")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        Category updatedCategory = Category.builder()
                .categoryId(existingCategory.getCategoryId())
                .categoryName(existingCategory.getCategoryName())
                .categoryStatus(existingCategory.getCategoryStatus())
                .createdAt(existingCategory.getCreatedAt())
                .updatedAt(TIMESTAMP_NOW)
                .build();

        CategoryResponse categoryResponse = CategoryResponse.builder()
                .categoryId(updatedCategory.getCategoryId())
                .categoryName(updatedCategory.getCategoryName())
                .categoryStatus(updatedCategory.getCategoryStatus())
                .createdAt(updatedCategory.getCreatedAt())
                .updatedAt(updatedCategory.getUpdatedAt())
                .build();

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(existingCategory)).thenReturn(updatedCategory);
        when(categoryMapper.categoryToResponse(updatedCategory)).thenReturn(categoryResponse);

        CategoryResponse actualResponse = categoryService.updateCategory(CATEGORY_ID.toString(), categoryRequest);

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);

        verify(categoryRepository, times(1)).save(categoryCaptor.capture());
        Category capturedCategory = categoryCaptor.getValue();
        assertNotNull(capturedCategory);
        assertEquals(existingCategory.getCategoryName(), capturedCategory.getCategoryName());
        assertEquals(CATEGORY_STATUS_ACTIVE, capturedCategory.getCategoryStatus());

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

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .categoryName("Updated Name")
                .build();

        when(categoryRepository.findById(NON_EXISTING_CATEGORY_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                categoryService.updateCategory(NON_EXISTING_CATEGORY_ID.toString(), categoryRequest));

        verify(categoryRepository, times(1)).findById(NON_EXISTING_CATEGORY_ID);
        verify(categoryRepository, never()).save(any(Category.class));
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));

        assertEquals(String.format("Category with id: %s, was not found.", NON_EXISTING_CATEGORY_ID), thrownException.getMessage());
    }

    @Test
    void updateCategory_shouldThrowIllegalArgumentExceptionWhenCategoryIsInactive() {

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .categoryName("Updated Name")
                .build();

        Category existingCategory = Category.builder()
                .categoryId(CATEGORY_ID)
                .categoryName("Original Name")
                .categoryStatus(CATEGORY_STATUS_INACTIVE)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existingCategory));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
                categoryService.updateCategory(CATEGORY_ID.toString(), categoryRequest));

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(categoryRepository, never()).save(any(Category.class));
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));

        assertEquals(String.format("Category with id: %s, is inactive and can not be updated.", CATEGORY_ID), thrownException.getMessage());
    }

    @Test
    void updateCategory_shouldThrowIllegalArgumentExceptionWhenCategoryIdIsInvalidUuidString() {

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .categoryName("Updated Name")
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                categoryService.updateCategory(INVALID_ID, categoryRequest));

        verify(categoryRepository, never()).findById(any(UUID.class));
        verify(categoryRepository, never()).save(any(Category.class));
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));
    }

    @Test
    void setCategoryStatus_shouldSetCategoryStatusSuccessfullyWhenCategoryExistsAndStatusIsDifferent() {

        Category existingCategory = Category.builder()
                .categoryId(CATEGORY_ID)
                .categoryName("Existing Category")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        Category updatedCategory = Category.builder()
                .categoryId(existingCategory.getCategoryId())
                .categoryName(existingCategory.getCategoryName())
                .categoryStatus(CATEGORY_STATUS_INACTIVE)
                .createdAt(existingCategory.getCreatedAt())
                .updatedAt(TIMESTAMP_NOW)
                .build();

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("Status '%s' was set for category with id: %s.", CATEGORY_STATUS_INACTIVE.name(), CATEGORY_ID))
                .build();

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(existingCategory)).thenReturn(updatedCategory);

        MessageResponse actualResponse = categoryService.setCategoryStatus(CATEGORY_ID.toString(), CATEGORY_STATUS_INACTIVE.name());

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(categoryRepository, times(1)).save(categoryCaptor.capture());
        Category capturedCategory = categoryCaptor.getValue();
        assertNotNull(capturedCategory);
        assertEquals(existingCategory.getCategoryName(), capturedCategory.getCategoryName());
        assertEquals(CATEGORY_STATUS_INACTIVE, capturedCategory.getCategoryStatus());
        assertTrue(updatedCategory.getUpdatedAt().isAfter(existingCategory.getUpdatedAt()));

        assertNotNull(actualResponse);
        assertEquals(messageResponse.getMessage(), actualResponse.getMessage());
    }

    @Test
    void setCategoryStatus_shouldThrowDataNotFoundExceptionWhenCategoryDoesNotExist() {

        when(categoryRepository.findById(NON_EXISTING_CATEGORY_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                categoryService.setCategoryStatus(NON_EXISTING_CATEGORY_ID.toString(), CATEGORY_STATUS_INACTIVE.name()));

        verify(categoryRepository, times(1)).findById(NON_EXISTING_CATEGORY_ID);
        verify(categoryRepository, never()).save(any(Category.class));

        assertEquals(String.format("Category with id: %s, was not found.", NON_EXISTING_CATEGORY_ID), thrownException.getMessage());
    }

    @Test
    void setCategoryStatus_shouldThrowIllegalArgumentExceptionWhenCategoryAlreadyHasTargetStatus() {

        String sameStatus = CategoryStatus.ACTIVE.name();

        Category existingCategory = Category.builder()
                .categoryId(CATEGORY_ID)
                .categoryName("Existing Category")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existingCategory));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
                categoryService.setCategoryStatus(CATEGORY_ID.toString(), sameStatus));

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(categoryRepository, never()).save(any(Category.class));

        assertEquals(String.format("Category with id: %s, already has status '%s'.", CATEGORY_ID, sameStatus), thrownException.getMessage());
    }

    @Test
    void setCategoryStatus_shouldThrowIllegalArgumentExceptionWhenInvalidUuidStringIsProvided() {

        assertThrows(IllegalArgumentException.class, () ->
                categoryService.setCategoryStatus(INVALID_ID, CATEGORY_STATUS_INACTIVE.name()));

        verify(categoryRepository, never()).findById(any(UUID.class));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void setCategoryStatus_shouldThrowIllegalArgumentExceptionWhenInvalidStatusStringIsProvided() {

        Category existingCategory = Category.builder()
                .categoryId(CATEGORY_ID)
                .categoryName("Existing Category")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existingCategory));

        assertThrows(IllegalArgumentException.class, () ->
                categoryService.setCategoryStatus(CATEGORY_ID.toString(), INVALID_STATUS));

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(categoryRepository, never()).save(any(Category.class));
    }
}