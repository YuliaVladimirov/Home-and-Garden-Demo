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

    private final Integer PAGE = 0;
    private final Integer SIZE = 5;
    private final String ORDER = "ASC";
    private final String SORT_BY = "createdAt";

    private final UUID CATEGORY_1_ID = UUID.randomUUID();
    private final UUID CATEGORY_2_ID = UUID.randomUUID();

    private final UUID CATEGORY_ID = UUID.randomUUID();
    private final String CATEGORY_ID_STRING = CATEGORY_ID.toString();
    private final UUID NON_EXISTING_CATEGORY_ID = UUID.randomUUID();
    private final String NON_EXISTING_CATEGORY_ID_STRING = NON_EXISTING_CATEGORY_ID.toString();
    private final String INVALID_ID = "Invalid UUID";

    private final CategoryStatus CATEGORY_STATUS_ACTIVE = CategoryStatus.ACTIVE;
    private final String CATEGORY_STATUS_ACTIVE_STRING = CATEGORY_STATUS_ACTIVE.name();
    private final CategoryStatus CATEGORY_STATUS_INACTIVE = CategoryStatus.INACTIVE;
    private final String CATEGORY_STATUS_INACTIVE_STRING = CATEGORY_STATUS_INACTIVE.name();
    private final String INVALID_STATUS = "Invalid Status";

    private final Instant CREATED_AT_NOW = Instant.now();
    private final Instant CREATED_AT_PAST = Instant.now().minus(10L, ChronoUnit.DAYS);
    private final Instant UPDATED_AT_NOW = Instant.now();
    private final Instant UPDATED_AT_PAST = Instant.now().minus(10L, ChronoUnit.DAYS);

    @Test
    void getAllActiveCategories_shouldReturnPagedActiveCategoriesWhenCategoriesExist() {

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        Category category1 = Category.builder()
                .categoryId(CATEGORY_1_ID)
                .categoryName("Category One")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(CREATED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        Category category2 = Category.builder()
                .categoryId(CATEGORY_2_ID)
                .categoryName("Category Two")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(CREATED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
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

        PagedModel<CategoryResponse> actualResponse = categoryService.getAllActiveCategories(SIZE, PAGE, ORDER, SORT_BY);

        verify(categoryRepository, times(1)).findAllByCategoryStatus(CATEGORY_STATUS_ACTIVE, pageRequest);
        verify(categoryMapper, times(1)).categoryToResponse(category1);
        verify(categoryMapper, times(1)).categoryToResponse(category2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(allCategories.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals((long)SIZE, actualResponse.getMetadata().size());
        assertEquals((long)PAGE, actualResponse.getMetadata().number());

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

        PagedModel<CategoryResponse> actualResponse = categoryService.getAllActiveCategories(SIZE, PAGE, ORDER, SORT_BY);

        verify(categoryRepository, times(1)).findAllByCategoryStatus(CATEGORY_STATUS_ACTIVE, pageRequest);
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(0L, actualResponse.getMetadata().totalElements());
        assertEquals(0L, actualResponse.getMetadata().totalPages());
        assertEquals((long)SIZE, actualResponse.getMetadata().size());
        assertEquals((long)PAGE, actualResponse.getMetadata().number());

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
                .createdAt(CREATED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        Category category2 = Category.builder()
                .categoryId(CATEGORY_2_ID)
                .categoryName("Category Two")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(CREATED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
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

        PagedModel<CategoryResponse> actualResponse = categoryService.getCategoriesByStatus(CATEGORY_STATUS_ACTIVE_STRING, SIZE, PAGE, ORDER, SORT_BY);

        verify(categoryRepository, times(1)).findAllByCategoryStatus(CATEGORY_STATUS_ACTIVE, pageRequest);
        verify(categoryMapper, times(1)).categoryToResponse(category1);
        verify(categoryMapper, times(1)).categoryToResponse(category2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(allCategories.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals((long)SIZE, actualResponse.getMetadata().size());
        assertEquals((long)PAGE, actualResponse.getMetadata().number());

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
                .createdAt(CREATED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        Category category2 = Category.builder()
                .categoryId(CATEGORY_2_ID)
                .categoryName("Category Two")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(CREATED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
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

        PagedModel<CategoryResponse> actualResponse = categoryService.getCategoriesByStatus(null, SIZE, PAGE, ORDER, SORT_BY);

        verify(categoryRepository, times(1)).findAll(pageRequest);
        verify(categoryMapper, times(1)).categoryToResponse(category1);
        verify(categoryMapper, times(1)).categoryToResponse(category2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(allCategories.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals((long)SIZE, actualResponse.getMetadata().size());
        assertEquals((long)PAGE, actualResponse.getMetadata().number());

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
                .createdAt(CREATED_AT_NOW)
                .updatedAt(UPDATED_AT_NOW)
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
        when(categoryRepository.saveAndFlush(categoryToSave)).thenReturn(savedCategory);
        when(categoryMapper.categoryToResponse(savedCategory)).thenReturn(categoryResponse);

        CategoryResponse actualResponse = categoryService.addCategory(categoryRequest);

        verify(categoryRepository, times(1)).existsByCategoryName(categoryRequest.getCategoryName());
        verify(categoryMapper, times(1)).requestToCategory(categoryRequest);

        verify(categoryRepository, times(1)).saveAndFlush(categoryCaptor.capture());
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
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
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
                .createdAt(CREATED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        Category updatedCategory = Category.builder()
                .categoryId(existingCategory.getCategoryId())
                .categoryName(categoryRequest.getCategoryName())
                .categoryStatus(existingCategory.getCategoryStatus())
                .createdAt(existingCategory.getCreatedAt())
                .updatedAt(UPDATED_AT_NOW)
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
        when(categoryRepository.saveAndFlush(existingCategory)).thenReturn(updatedCategory);
        when(categoryMapper.categoryToResponse(updatedCategory)).thenReturn(categoryResponse);

        CategoryResponse actualResponse = categoryService.updateCategory(CATEGORY_ID_STRING, categoryRequest);

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(categoryRepository, times(1)).saveAndFlush(categoryCaptor.capture());
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
                .createdAt(CREATED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        Category updatedCategory = Category.builder()
                .categoryId(existingCategory.getCategoryId())
                .categoryName(existingCategory.getCategoryName())
                .categoryStatus(existingCategory.getCategoryStatus())
                .createdAt(existingCategory.getCreatedAt())
                .updatedAt(UPDATED_AT_NOW)
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
        when(categoryRepository.saveAndFlush(existingCategory)).thenReturn(updatedCategory);
        when(categoryMapper.categoryToResponse(updatedCategory)).thenReturn(categoryResponse);

        CategoryResponse actualResponse = categoryService.updateCategory(CATEGORY_ID_STRING, categoryRequest);

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);

        verify(categoryRepository, times(1)).saveAndFlush(categoryCaptor.capture());
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
                categoryService.updateCategory(NON_EXISTING_CATEGORY_ID_STRING, categoryRequest));

        verify(categoryRepository, times(1)).findById(NON_EXISTING_CATEGORY_ID);
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));

        assertEquals(String.format("Category with id: %s, was not found.", NON_EXISTING_CATEGORY_ID_STRING), thrownException.getMessage());
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
                .createdAt(CREATED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existingCategory));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
                categoryService.updateCategory(CATEGORY_ID_STRING, categoryRequest));

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));

        assertEquals(String.format("Category with id: %s, is inactive and can not be updated.", CATEGORY_ID_STRING), thrownException.getMessage());
    }

    @Test
    void updateCategory_shouldThrowIllegalArgumentExceptionWhenCategoryIdIsInvalidUuidString() {

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .categoryName("Updated Name")
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                categoryService.updateCategory(INVALID_ID, categoryRequest));

        verify(categoryRepository, never()).findById(any(UUID.class));
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));
    }

    @Test
    void setCategoryStatus_shouldSetCategoryStatusSuccessfullyWhenCategoryExistsAndStatusIsDifferent() {

        Category existingCategory = Category.builder()
                .categoryId(CATEGORY_ID)
                .categoryName("Existing Category")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(CREATED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        Category updatedCategory = Category.builder()
                .categoryId(existingCategory.getCategoryId())
                .categoryName(existingCategory.getCategoryName())
                .categoryStatus(CATEGORY_STATUS_INACTIVE)
                .createdAt(existingCategory.getCreatedAt())
                .updatedAt(UPDATED_AT_NOW)
                .build();

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("Status '%s' was set for category with id: %s.", CATEGORY_STATUS_INACTIVE_STRING, CATEGORY_ID_STRING))
                .build();

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.saveAndFlush(existingCategory)).thenReturn(updatedCategory);

        MessageResponse actualResponse = categoryService.setCategoryStatus(CATEGORY_ID_STRING, CATEGORY_STATUS_INACTIVE_STRING);

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(categoryRepository, times(1)).saveAndFlush(categoryCaptor.capture());
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
                categoryService.setCategoryStatus(NON_EXISTING_CATEGORY_ID_STRING, CATEGORY_STATUS_INACTIVE_STRING));

        verify(categoryRepository, times(1)).findById(NON_EXISTING_CATEGORY_ID);
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));

        assertEquals(String.format("Category with id: %s, was not found.", NON_EXISTING_CATEGORY_ID_STRING), thrownException.getMessage());
    }

    @Test
    void setCategoryStatus_shouldThrowIllegalArgumentExceptionWhenCategoryAlreadyHasTargetStatus() {

        String sameStatus = CategoryStatus.ACTIVE.name();

        Category existingCategory = Category.builder()
                .categoryId(CATEGORY_ID)
                .categoryName("Existing Category")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(CREATED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existingCategory));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
                categoryService.setCategoryStatus(CATEGORY_ID_STRING, sameStatus));

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));

        assertEquals(String.format("Category with id: %s, already has status '%s'.", CATEGORY_ID_STRING, sameStatus), thrownException.getMessage());
    }

    @Test
    void setCategoryStatus_shouldThrowIllegalArgumentExceptionWhenInvalidUuidStringIsProvided() {

        assertThrows(IllegalArgumentException.class, () ->
                categoryService.setCategoryStatus(INVALID_ID, CATEGORY_STATUS_INACTIVE_STRING));

        verify(categoryRepository, never()).findById(any(UUID.class));
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
    }

    @Test
    void setCategoryStatus_shouldThrowIllegalArgumentExceptionWhenInvalidStatusStringIsProvided() {

        Category existingCategory = Category.builder()
                .categoryId(CATEGORY_ID)
                .categoryName("Existing Category")
                .categoryStatus(CATEGORY_STATUS_ACTIVE)
                .createdAt(CREATED_AT_PAST)
                .updatedAt(UPDATED_AT_PAST)
                .build();

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existingCategory));

        assertThrows(IllegalArgumentException.class, () ->
                categoryService.setCategoryStatus(CATEGORY_ID_STRING, INVALID_STATUS));

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
    }
}