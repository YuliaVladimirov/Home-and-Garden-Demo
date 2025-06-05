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

    @Test
    void getAllActiveCategories_shouldReturnPagedActiveCategoriesWhenCategoriesExist() {

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "createdAt";

        Pageable pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);

        Category category1 = Category.builder()
                .categoryId(UUID.randomUUID())
                .categoryName("Pots and planters")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .products(new HashSet<>())
                .build();

        Category category2 = Category.builder()
                .categoryId(UUID.randomUUID())
                .categoryName("Tools and equipment")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .products(new HashSet<>())
                .build();

        List<Category> allCategories = List.of(category1, category2);
        Page<Category> pageIml = new PageImpl<>(allCategories, pageRequest, allCategories.size());

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

        long expectedTotalPages = (long) Math.ceil((double) allCategories.size() / size);

        when(categoryRepository.findAllByCategoryStatusIs(CategoryStatus.ACTIVE, pageRequest)).thenReturn(pageIml);
        when(categoryMapper.categoryToResponse(category1)).thenReturn(categoryResponse1);
        when(categoryMapper.categoryToResponse(category2)).thenReturn(categoryResponse2);

        PagedModel<CategoryResponse> retrievedCategories = categoryService.getAllActiveCategories(size, page, order, sortBy);

        verify(categoryRepository, times(1)).findAllByCategoryStatusIs(CategoryStatus.ACTIVE, pageRequest);
        verify(categoryMapper, times(1)).categoryToResponse(category1);
        verify(categoryMapper, times(1)).categoryToResponse(category2);

        assertNotNull(retrievedCategories);
        assertNotNull(retrievedCategories.getMetadata());
        assertEquals(allCategories.size(), retrievedCategories.getMetadata().totalElements());
        assertEquals(expectedTotalPages, retrievedCategories.getMetadata().totalPages());
        assertEquals(size, retrievedCategories.getMetadata().size());
        assertEquals(page, retrievedCategories.getMetadata().number());

        assertNotNull(retrievedCategories.getContent());
        assertEquals(allCategories.size(), retrievedCategories.getContent().size());
        assertEquals(categoryResponse1.getCategoryId(), retrievedCategories.getContent().get(0).getCategoryId());
        assertEquals(categoryResponse1.getCategoryName(), retrievedCategories.getContent().get(0).getCategoryName());
        assertEquals(CategoryStatus.ACTIVE, retrievedCategories.getContent().get(0).getCategoryStatus());
        assertEquals(categoryResponse2.getCategoryId(), retrievedCategories.getContent().get(1).getCategoryId());
        assertEquals(categoryResponse2.getCategoryName(), retrievedCategories.getContent().get(1).getCategoryName());
        assertEquals(CategoryStatus.ACTIVE, retrievedCategories.getContent().get(1).getCategoryStatus());
    }

    @Test
    void getAllActiveCategories_shouldReturnEmptyPagedModelWhenNoActiveCategoriesExist() {

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "createdAt";

        Pageable pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);
        Page<Category> emptyPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(categoryRepository.findAllByCategoryStatusIs(CategoryStatus.ACTIVE, pageRequest)).thenReturn(emptyPage);

        PagedModel<CategoryResponse> retrievedCategories = categoryService.getAllActiveCategories(size, page, order, sortBy);

        verify(categoryRepository, times(1)).findAllByCategoryStatusIs(CategoryStatus.ACTIVE, pageRequest);
        verify(categoryMapper, never()).categoryToResponse(any());

        assertNotNull(retrievedCategories);
        assertNotNull(retrievedCategories.getMetadata());
        assertEquals(0L, retrievedCategories.getMetadata().totalElements());
        assertEquals(0L, retrievedCategories.getMetadata().totalPages());
        assertEquals(size, retrievedCategories.getMetadata().size());
        assertEquals(page, retrievedCategories.getMetadata().number());

        assertNotNull(retrievedCategories.getContent());
        assertTrue(retrievedCategories.getContent().isEmpty());
        assertEquals(0, retrievedCategories.getContent().size());
    }

    @Test
    void getAllActiveCategories_shouldPassCorrectPageableToRepository() {

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "createdAt";

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(categoryRepository.findAllByCategoryStatusIs(eq(CategoryStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(Page.empty());

        categoryService.getAllActiveCategories(size, page, order, sortBy);

        verify(categoryRepository).findAllByCategoryStatusIs(eq(CategoryStatus.ACTIVE), pageableCaptor.capture());

        Pageable usedPageable = pageableCaptor.getValue();
        assertEquals(page, usedPageable.getPageNumber());
        assertEquals(size, usedPageable.getPageSize());

        Sort.Order sortOrder = usedPageable.getSort().getOrderFor(sortBy);
        assertNotNull(sortOrder);
        assertEquals(Sort.Direction.ASC, sortOrder.getDirection());
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

        Category category1 = Category.builder()
                .categoryId(UUID.randomUUID())
                .categoryName("Pots and planters")
                .categoryStatus(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .products(new HashSet<>())
                .build();

        Category category2 = Category.builder()
                .categoryId(UUID.randomUUID())
                .categoryName("Tools and equipment")
                .categoryStatus(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .products(new HashSet<>())
                .build();

        List<Category> allCategories = List.of(category1, category2);
        Page<Category> pageIml = new PageImpl<>(allCategories, pageRequest, allCategories.size());

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

        long expectedTotalPages = (long) Math.ceil((double) allCategories.size() / size);

        when(categoryRepository.findAllByCategoryStatusIs(status, pageRequest)).thenReturn(pageIml);
        when(categoryMapper.categoryToResponse(category1)).thenReturn(categoryResponse1);
        when(categoryMapper.categoryToResponse(category2)).thenReturn(categoryResponse2);

        PagedModel<CategoryResponse> retrievedCategories = categoryService.getCategoriesByStatus(categoryStatus, size, page, order, sortBy);

        verify(categoryRepository, times(1)).findAllByCategoryStatusIs(status, pageRequest);
        verify(categoryMapper, times(1)).categoryToResponse(category1);
        verify(categoryMapper, times(1)).categoryToResponse(category2);

        assertNotNull(retrievedCategories);
        assertNotNull(retrievedCategories.getMetadata());
        assertEquals(allCategories.size(), retrievedCategories.getMetadata().totalElements());
        assertEquals(expectedTotalPages, retrievedCategories.getMetadata().totalPages());
        assertEquals(size, retrievedCategories.getMetadata().size());
        assertEquals(page, retrievedCategories.getMetadata().number());

        assertNotNull(retrievedCategories.getContent());
        assertEquals(allCategories.size(), retrievedCategories.getContent().size());
        assertEquals(categoryResponse1.getCategoryId(), retrievedCategories.getContent().get(0).getCategoryId());
        assertEquals(categoryResponse1.getCategoryName(), retrievedCategories.getContent().get(0).getCategoryName());
        assertEquals(categoryResponse2.getCategoryStatus(), retrievedCategories.getContent().get(0).getCategoryStatus());
        assertEquals(categoryResponse2.getCategoryId(), retrievedCategories.getContent().get(1).getCategoryId());
        assertEquals(categoryResponse2.getCategoryName(), retrievedCategories.getContent().get(1).getCategoryName());
        assertEquals(categoryResponse2.getCategoryStatus(), retrievedCategories.getContent().get(1).getCategoryStatus());
    }

    @Test
    void getCategoriesByStatus_shouldRetrieveAndMapAllCategoriesWithPaginationIfNoStatusIsProvided() {

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "createdAt";

        Pageable pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);

        Category category1 = Category.builder()
                .categoryId(UUID.randomUUID())
                .categoryName("Pots and planters")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .products(new HashSet<>())
                .build();

        Category category2 = Category.builder()
                .categoryId(UUID.randomUUID())
                .categoryName("Tools and equipment")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .products(new HashSet<>())
                .build();

        List<Category> allCategories = List.of(category1, category2);
        Page<Category> pageIml = new PageImpl<>(allCategories, pageRequest, allCategories.size());

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

        long expectedTotalPages = (long) Math.ceil((double) allCategories.size() / size);

        when(categoryRepository.findAll(pageRequest)).thenReturn(pageIml);
        when(categoryMapper.categoryToResponse(category1)).thenReturn(categoryResponse1);
        when(categoryMapper.categoryToResponse(category2)).thenReturn(categoryResponse2);

        PagedModel<CategoryResponse> retrievedCategories = categoryService.getCategoriesByStatus(null, size, page, order, sortBy);

        verify(categoryRepository, times(1)).findAll(pageRequest);
        verify(categoryMapper, times(1)).categoryToResponse(category1);
        verify(categoryMapper, times(1)).categoryToResponse(category2);

        assertNotNull(retrievedCategories);
        assertNotNull(retrievedCategories.getMetadata());
        assertEquals(allCategories.size(), retrievedCategories.getMetadata().totalElements());
        assertEquals(expectedTotalPages, retrievedCategories.getMetadata().totalPages());
        assertEquals(size, retrievedCategories.getMetadata().size());
        assertEquals(page, retrievedCategories.getMetadata().number());

        assertNotNull(retrievedCategories.getContent());
        assertEquals(allCategories.size(), retrievedCategories.getContent().size());
        assertEquals(categoryResponse1.getCategoryId(), retrievedCategories.getContent().get(0).getCategoryId());
        assertEquals(categoryResponse1.getCategoryName(), retrievedCategories.getContent().get(0).getCategoryName());
        assertEquals(categoryResponse2.getCategoryStatus(), retrievedCategories.getContent().get(0).getCategoryStatus());
        assertEquals(categoryResponse2.getCategoryId(), retrievedCategories.getContent().get(1).getCategoryId());
        assertEquals(categoryResponse2.getCategoryName(), retrievedCategories.getContent().get(1).getCategoryName());
        assertEquals(categoryResponse2.getCategoryStatus(), retrievedCategories.getContent().get(1).getCategoryStatus());
    }

    @Test
    void getCategoriesByStatus_shouldPassCorrectPageableToRepository() {

        CategoryStatus status = CategoryStatus.INACTIVE;
        String categoryStatus = status.name();

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "createdAt";

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(categoryRepository.findAllByCategoryStatusIs(eq(status), any(Pageable.class)))
                .thenReturn(Page.empty());

        categoryService.getCategoriesByStatus(categoryStatus, size, page, order, sortBy);

        verify(categoryRepository).findAllByCategoryStatusIs(eq(status), pageableCaptor.capture());

        Pageable usedPageable = pageableCaptor.getValue();
        assertEquals(page, usedPageable.getPageNumber());
        assertEquals(size, usedPageable.getPageSize());

        Sort.Order sortOrder = usedPageable.getSort().getOrderFor(sortBy);
        assertNotNull(sortOrder);
        assertEquals(Sort.Direction.ASC, sortOrder.getDirection());
    }

    @Test
    void getCategoriesByStatus_shouldPassCorrectPageableToRepositoryIfNoStatusProvided() {

        int page = 0;
        int size = 5;
        String order = "ASC";
        String sortBy = "createdAt";

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(categoryRepository.findAll(any(Pageable.class)))
                .thenReturn(Page.empty());

        categoryService.getCategoriesByStatus(null, size, page, order, sortBy);

        verify(categoryRepository).findAll(pageableCaptor.capture());

        Pageable usedPageable = pageableCaptor.getValue();
        assertEquals(page, usedPageable.getPageNumber());
        assertEquals(size, usedPageable.getPageSize());

        Sort.Order sortOrder = usedPageable.getSort().getOrderFor(sortBy);
        assertNotNull(sortOrder);
        assertEquals(Sort.Direction.ASC, sortOrder.getDirection());
    }

    @Test
    void addCategory_shouldAddCategorySuccessfullyWhenCategoryNameDoesNotExist() {

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .categoryName("New Category")
                .build();

        Category categoryToSave = Category.builder()
                .categoryName(categoryRequest.getCategoryName())
                .categoryStatus(CategoryStatus.ACTIVE)
                .products(new HashSet<>())
                .build();

        Category savedCategory = Category.builder()
                .categoryId(UUID.randomUUID())
                .categoryName(categoryToSave.getCategoryName())
                .categoryStatus(categoryToSave.getCategoryStatus())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .products(categoryToSave.getProducts())
                .build();

        CategoryResponse categoryResponse = CategoryResponse.builder()
                .categoryId(savedCategory.getCategoryId())
                .categoryName(savedCategory.getCategoryName())
                .categoryStatus(savedCategory.getCategoryStatus())
                .createdAt(savedCategory.getCreatedAt())
                .updatedAt(savedCategory.getUpdatedAt())
                .build();

        when(categoryRepository.existsByCategoryName(categoryRequest.getCategoryName())).thenReturn(false);
        when(categoryMapper.requestToCategory(categoryRequest)).thenReturn(categoryToSave);
        when(categoryRepository.saveAndFlush(categoryToSave)).thenReturn(savedCategory);
        when(categoryMapper.categoryToResponse(savedCategory)).thenReturn(categoryResponse);

        CategoryResponse actualResponse = categoryService.addCategory(categoryRequest);

        verify(categoryRepository, times(1)).existsByCategoryName(categoryRequest.getCategoryName());
        verify(categoryMapper, times(1)).requestToCategory(categoryRequest);
        verify(categoryRepository, times(1)).saveAndFlush(categoryToSave);
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

        Category existingCategory = Category.builder()
                .categoryId(id)
                .categoryName("Original Name")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(3600))
                .build();

        Category updatedCategory = Category.builder()
                .categoryId(id)
                .categoryName("Updated Name")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now())
                .build();

        CategoryResponse categoryResponse = CategoryResponse.builder()
                .categoryId(updatedCategory.getCategoryId())
                .categoryName(updatedCategory.getCategoryName())
                .categoryStatus(updatedCategory.getCategoryStatus())
                .createdAt(updatedCategory.getCreatedAt())
                .updatedAt(updatedCategory.getUpdatedAt())
                .build();

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.saveAndFlush(any(Category.class))).thenReturn(updatedCategory);
        when(categoryMapper.categoryToResponse(updatedCategory)).thenReturn(categoryResponse);

        CategoryResponse actualResponse = categoryService.updateCategory(categoryId, categoryRequest);

        verify(categoryRepository, times(1)).findById(id);
        verify(categoryRepository, times(1)).saveAndFlush(any(Category.class));
        verify(categoryMapper, times(1)).categoryToResponse(updatedCategory);

        assertNotNull(actualResponse);
        assertEquals(categoryResponse.getCategoryId(), actualResponse.getCategoryId());
        assertEquals(categoryResponse.getCategoryName(), actualResponse.getCategoryName());
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

        Category existingCategory = Category.builder()
                .categoryId(id)
                .categoryName("Original Name")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(3600))
                .build();

        Category updatedCategory = Category.builder()
                .categoryId(id)
                .categoryName("Original Name")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now())
                .build();

        CategoryResponse categoryResponse = CategoryResponse.builder()
                .categoryId(updatedCategory.getCategoryId())
                .categoryName(updatedCategory.getCategoryName())
                .categoryStatus(updatedCategory.getCategoryStatus())
                .createdAt(updatedCategory.getCreatedAt())
                .updatedAt(updatedCategory.getUpdatedAt())
                .build();

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.saveAndFlush(any(Category.class))).thenReturn(updatedCategory);
        when(categoryMapper.categoryToResponse(updatedCategory)).thenReturn(categoryResponse);

        CategoryResponse actualResponse = categoryService.updateCategory(categoryId, categoryRequest);

        verify(categoryRepository, times(1)).findById(id);
        verify(categoryRepository, times(1)).saveAndFlush(any(Category.class));
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

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .categoryName("Updated Name")
                .build();

        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
            categoryService.updateCategory(categoryId, categoryRequest));

        verify(categoryRepository, times(1)).findById(id);
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));

        assertEquals(String.format("Category with id: %s, was not found.", categoryId), thrownException.getMessage());
    }

    @Test
    void updateCategory_shouldThrowIllegalArgumentExceptionWhenCategoryIsInactive() {

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .categoryName("Updated Name")
                .build();

        Category existingCategory = Category.builder()
                .categoryId(id)
                .categoryName("Original Name")
                .categoryStatus(CategoryStatus.INACTIVE)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(3600))
                .build();

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
            categoryService.updateCategory(categoryId, categoryRequest));

        verify(categoryRepository, times(1)).findById(id);
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
        verify(categoryMapper, never()).categoryToResponse(any(Category.class));

        assertEquals(String.format("Category with id: %s, is inactive and can not be updated.", categoryId), thrownException.getMessage());
    }

    @Test
    void setCategoryStatus_shouldSetCategoryStatusSuccessfullyWhenCategoryExistsAndStatusIsDifferent() {

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        CategoryStatus newStatus = CategoryStatus.INACTIVE;
        String categoryNewStatus = newStatus.toString();

        Category existingCategory = Category.builder()
                .categoryId(id)
                .categoryName("Original Name")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(3600))
                .build();

        Category updatedCategory = Category.builder()
                .categoryId(id)
                .categoryName("Original Name")
                .categoryStatus(newStatus)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now())
                .build();

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.saveAndFlush(any(Category.class))).thenReturn(updatedCategory);

        MessageResponse response = categoryService.setCategoryStatus(categoryId, categoryNewStatus);

        verify(categoryRepository, times(1)).findById(id);
        verify(categoryRepository, times(1)).saveAndFlush(any(Category.class));

        assertNotNull(response);
        assertEquals(String.format("Status '%s' was set for category with id: %s.", categoryNewStatus.toUpperCase(), categoryId), response.getMessage());
    }

    @Test
    void setCategoryStatus_shouldThrowDataNotFoundExceptionWhenCategoryDoesNotExist() {

        UUID nonExistingId = UUID.randomUUID();
        String nonExistingCategoryId = nonExistingId.toString();

        String categoryTargetStatus = CategoryStatus.INACTIVE.name();

        when(categoryRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
            categoryService.setCategoryStatus(nonExistingCategoryId, categoryTargetStatus));

        verify(categoryRepository, times(1)).findById(nonExistingId);
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));

        assertEquals(String.format("Category with id: %s, was not found.", nonExistingCategoryId), thrownException.getMessage());
    }

    @Test
    void setCategoryStatus_shouldThrowIllegalArgumentExceptionWhenCategoryAlreadyHasTargetStatus() {

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        String categoryTargetStatus = CategoryStatus.ACTIVE.name();

        Category existingCategory = Category.builder()
                .categoryId(id)
                .categoryName("Original Name")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(3600))
                .build();

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
            categoryService.setCategoryStatus(categoryId, categoryTargetStatus));

        verify(categoryRepository, times(1)).findById(id);
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));

        assertEquals(String.format("Category with id: %s, already has status '%s'.", categoryId, categoryTargetStatus.toUpperCase()), thrownException.getMessage());
    }

    @Test
    void setCategoryStatus_shouldThrowIllegalArgumentExceptionWhenInvalidStatusStringIsProvided() {

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        String invalidStatusString = "NOT_VALID_STATUS";

        Category existingCategory = Category.builder()
                .categoryId(id)
                .categoryName("Original Name")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(3600))
                .build();

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));

        assertThrows(IllegalArgumentException.class, () ->
            categoryService.setCategoryStatus(categoryId, invalidStatusString));

        verify(categoryRepository, times(1)).findById(id);
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
    }

    @Test
    void setCategoryStatus_shouldThrowIllegalStateExceptionWhenStatusUpdateFailsOnSave() {

        UUID id = UUID.randomUUID();
        String categoryId = id.toString();

        String categoryNewStatus = CategoryStatus.INACTIVE.name();

        Category existingCategory = Category.builder()
                .categoryId(id)
                .categoryName("Original Name")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(3600))
                .build();

        Category savedCategoryWithOriginalStatus = Category.builder()
                .categoryId(id)
                .categoryName("Original Name")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now())
                .build();

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.saveAndFlush(any(Category.class))).thenReturn(savedCategoryWithOriginalStatus);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
            categoryService.setCategoryStatus(categoryId, categoryNewStatus));

        verify(categoryRepository, times(1)).findById(id);
        verify(categoryRepository, times(1)).saveAndFlush(any(Category.class));

        assertEquals(String.format("Unfortunately something went wrong and status '%s' was not set for category with id: %s. Please, try again.", categoryNewStatus.toUpperCase(), categoryId), thrown.getMessage());
    }
}