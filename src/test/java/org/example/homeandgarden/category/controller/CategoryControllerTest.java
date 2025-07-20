package org.example.homeandgarden.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.homeandgarden.category.dto.CategoryRequest;
import org.example.homeandgarden.category.dto.CategoryResponse;
import org.example.homeandgarden.category.entity.enums.CategoryStatus;
import org.example.homeandgarden.category.service.CategoryServiceImpl;
import org.example.homeandgarden.product.dto.ProductResponse;
import org.example.homeandgarden.product.entity.enums.ProductStatus;
import org.example.homeandgarden.product.service.ProductServiceImpl;
import org.example.homeandgarden.shared.MessageResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(CategoryControllerTest.TestConfig.class)
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryServiceImpl categoryService;

    @Autowired
    private ProductServiceImpl productService;

    @TestConfiguration
    static class TestConfig {

        @Bean @Primary
        public CategoryServiceImpl categoryService() {
            return mock(CategoryServiceImpl.class);
        }

        @Bean @Primary
        public ProductServiceImpl productService() {
            return mock(ProductServiceImpl.class);
        }
    }

    @AfterEach
    void resetMocks() {
        reset(categoryService, productService);
    }

    @Test
    void getAllActiveCategories_shouldReturnPagedCategories_whenValidParameters() throws Exception {

        CategoryResponse categoryResponse1 = CategoryResponse.builder()
                .categoryId(UUID.randomUUID())
                .categoryName("Test Category One")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();

        CategoryResponse categoryResponse2 = CategoryResponse.builder()
                .categoryId(UUID.randomUUID())
                .categoryName("Test Category Two")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();

        List<CategoryResponse> content = Arrays.asList(categoryResponse1, categoryResponse2);
        Page<CategoryResponse> mockPage = new PageImpl<>(content, PageRequest.of(0, 2), 100);

        when(categoryService.getAllActiveCategories(eq(2), eq(0), eq("DESC"), eq("categoryName"))).thenReturn(mockPage);

        mockMvc.perform(get("/categories")
                        .param("size", "2")
                        .param("page", "0")
                        .param("order", "DESC")
                        .param("sortBy", "categoryName")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].categoryName").value("Test Category One"))
                .andExpect(jsonPath("$.content[0].categoryStatus").value(CategoryStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[1].categoryName").value("Test Category Two"))
                .andExpect(jsonPath("$.content[1].categoryStatus").value(CategoryStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.pageable.pageSize").value(2))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(100))
                .andExpect(jsonPath("$.totalPages").value(50));

        verify(categoryService, times(1)).getAllActiveCategories(eq(2), eq(0), eq("DESC"), eq("categoryName"));
    }

    @Test
    void getAllActiveCategories_shouldReturnPagedCategories_whenDefaultParameters() throws Exception {

        CategoryResponse categoryResponse1 = CategoryResponse.builder()
                .categoryId(UUID.randomUUID())
                .categoryName("Test Category One")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();

        CategoryResponse categoryResponse2 = CategoryResponse.builder()
                .categoryId(UUID.randomUUID())
                .categoryName("Test Category Two")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();

        List<CategoryResponse> content = Arrays.asList(categoryResponse1, categoryResponse2);
        Page<CategoryResponse> mockPage = new PageImpl<>(content, PageRequest.of(0, 10), 50);

        when(categoryService.getAllActiveCategories(eq(10), eq(0), eq("ASC"), eq("createdAt")))
                .thenReturn(mockPage);

        mockMvc.perform(get("/categories")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].categoryName").value("Test Category One"))
                .andExpect(jsonPath("$.content[0].categoryStatus").value(CategoryStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[1].categoryName").value("Test Category Two"))
                .andExpect(jsonPath("$.content[1].categoryStatus").value(CategoryStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(50))
                .andExpect(jsonPath("$.totalPages").value(5));

        verify(categoryService, times(1)).getAllActiveCategories(eq(10), eq(0), eq("ASC"), eq("createdAt"));
    }

    @Test
    void getAllActiveCategories_shouldReturnBadRequest_whenInvalidSize() throws Exception {

        mockMvc.perform(get("/categories")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Size must be greater than or equal to 1")))
                .andExpect(jsonPath("$.path").value("/categories"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).getAllActiveCategories(any(), any(), any(), any());
    }

    @Test
    void getAllActiveCategories_shouldReturnBadRequestWhenInvalidPage() throws Exception {

        mockMvc.perform(get("/categories")
                        .param("page", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Page numeration starts from 0")))
                .andExpect(jsonPath("$.path").value("/categories"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).getAllActiveCategories(any(), any(), any(), any());
    }

    @Test
    void getAllActiveCategories_shouldReturnBadRequest_whenInvalidOrder() throws Exception {
        mockMvc.perform(get("/categories")
                        .param("order", "INVALID_ORDER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order: Must be 'ASC' or 'DESC' ('asc' or 'desc')")))
                .andExpect(jsonPath("$.path").value("/categories"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).getAllActiveCategories(any(), any(), any(), any());
    }

    @Test
    void getAllActiveCategories_shouldReturnBadRequest_whenInvalidSortBy() throws Exception {
        mockMvc.perform(get("/categories")
                        .param("sortBy", "INVALID_SORT_BY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid value: Must be one of the following: 'categoryName', 'createdAt', 'updatedAt'")))
                .andExpect(jsonPath("$.path").value("/categories"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).getAllActiveCategories(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getCategoriesByStatus_shouldReturnPagedCategories_whenValidParameters() throws Exception {

        CategoryResponse categoryResponse1 = CategoryResponse.builder()
                .categoryId(UUID.randomUUID())
                .categoryName("Inactive Category One")
                .categoryStatus(CategoryStatus.INACTIVE)
                .createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();

        CategoryResponse categoryResponse2 = CategoryResponse.builder()
                .categoryId(UUID.randomUUID())
                .categoryName("Inactive Category Two")
                .categoryStatus(CategoryStatus.INACTIVE)
                .createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();

        List<CategoryResponse> content = Arrays.asList(categoryResponse1, categoryResponse2);
        Page<CategoryResponse> mockPage = new PageImpl<>(content, PageRequest.of(0, 2), 50);

        when(categoryService.getCategoriesByStatus(eq("INACTIVE"), eq(2), eq(0), eq("DESC"), eq("categoryName")))
                .thenReturn(mockPage);

        mockMvc.perform(get("/categories/status")
                        .param("categoryStatus", "INACTIVE")
                        .param("size", "2")
                        .param("page", "0")
                        .param("order", "DESC")
                        .param("sortBy", "categoryName")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].categoryName").value("Inactive Category One"))
                .andExpect(jsonPath("$.content[0].categoryStatus").value(CategoryStatus.INACTIVE.name()))
                .andExpect(jsonPath("$.content[1].categoryName").value("Inactive Category Two"))
                .andExpect(jsonPath("$.content[1].categoryStatus").value(CategoryStatus.INACTIVE.name()))
                .andExpect(jsonPath("$.pageable.pageSize").value(2))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(50))
                .andExpect(jsonPath("$.totalPages").value(25));

        verify(categoryService, times(1)).getCategoriesByStatus(eq("INACTIVE"), eq(2), eq(0), eq("DESC"), eq("categoryName"));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getCategoriesByStatus_shouldReturnPagedCategories_whenStatusOmittedAndDefaultParameters() throws Exception {

        CategoryResponse categoryResponse1 = CategoryResponse.builder()
                .categoryId(UUID.randomUUID())
                .categoryName("Active Category One")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();

        CategoryResponse categoryResponse2 = CategoryResponse.builder()
                .categoryId(UUID.randomUUID())
                .categoryName("Active Category Two")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();

        List<CategoryResponse> content = Arrays.asList(categoryResponse1, categoryResponse2);
        Page<CategoryResponse> mockPage = new PageImpl<>(content, PageRequest.of(0, 10), 50);

        when(categoryService.getCategoriesByStatus(eq(null), eq(10), eq(0), eq("ASC"), eq("createdAt")))
                .thenReturn(mockPage);

        mockMvc.perform(get("/categories/status")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].categoryName").value("Active Category One"))
                .andExpect(jsonPath("$.content[0].categoryStatus").value(CategoryStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[1].categoryName").value("Active Category Two"))
                .andExpect(jsonPath("$.content[1].categoryStatus").value(CategoryStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(50))
                .andExpect(jsonPath("$.totalPages").value(5));

        verify(categoryService, times(1)).getCategoriesByStatus(eq(null), eq(10), eq(0), eq("ASC"), eq("createdAt"));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getCategoriesByStatus_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        mockMvc.perform(get("/categories/status")
                        .param("categoryStatus", "ACTIVE")
                        .param("size", "2")
                        .param("page", "0")
                        .param("order", "ASC")
                        .param("sortBy", "createdAt")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").value("/categories/status"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).getCategoriesByStatus(any(), any(), any(), any(), any());
    }

    @Test
    void getCategoriesByStatus_shouldReturnUnauthorized_whenNoAuthentication() throws Exception {

        mockMvc.perform(get("/categories/status")
                        .param("categoryStatus", "ACTIVE")
                        .param("size", "2")
                        .param("page", "0")
                        .param("order", "ASC")
                        .param("sortBy", "createdAt")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/categories/status"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).getCategoriesByStatus(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getCategoriesByStatus_shouldReturnBadRequest_whenInvalidCategoryStatus() throws Exception {

        mockMvc.perform(get("/categories/status")
                        .param("categoryStatus", "INVALID_STATUS")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order categoryStatus: Must be one of the: 'ACTIVE' or 'INACTIVE' or ('active' or 'inactive')")))
                .andExpect(jsonPath("$.path").value("/categories/status"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).getCategoriesByStatus(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getCategoriesByStatus_shouldReturnBadRequest_whenInvalidSize() throws Exception {

        mockMvc.perform(get("/categories/status")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Size must be greater than or equal to 1")))
                .andExpect(jsonPath("$.path").value("/categories/status"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).getAllActiveCategories(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getCategoriesByStatus_shouldReturnBadRequest_whenInvalidPage() throws Exception {

        mockMvc.perform(get("/categories/status")
                        .param("page", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Page numeration starts from 0")))
                .andExpect(jsonPath("$.path").value("/categories/status"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).getAllActiveCategories(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getCategoriesByStatus_shouldReturnBadRequest_whenInvalidOrder() throws Exception {

        mockMvc.perform(get("/categories/status")
                        .param("order", "INVALID_ORDER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order: Must be 'ASC' or 'DESC' ('asc' or 'desc')")))
                .andExpect(jsonPath("$.path").value("/categories/status"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).getAllActiveCategories(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getCategoriesByStatus_shouldReturnBadRequest_whenInvalidSortBy() throws Exception {

        mockMvc.perform(get("/categories/status")
                        .param("sortBy", "INVALID_SORT_BY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid value: Must be one of the following: 'categoryName', 'createdAt', 'updatedAt'")))
                .andExpect(jsonPath("$.path").value("/categories/status"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).getAllActiveCategories(any(), any(), any(), any());
    }

    @Test
    void getCategoryProducts_shouldReturnPagedProducts_whenValidParameters() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        ProductResponse productResponse1 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(35.00))
                .productStatus(ProductStatus.AVAILABLE)
                .addedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        ProductResponse productResponse2 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Two")
                .listPrice(BigDecimal.valueOf(50.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(ProductStatus.AVAILABLE)
                .addedAt(Instant.now().minus(11, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .build();

        List<ProductResponse> content = Arrays.asList(productResponse1, productResponse2);
        Page<ProductResponse> mockPage = new PageImpl<>(content, PageRequest.of(0, 2), 50);

        when(productService.getCategoryProducts(
                eq(validCategoryId),
                eq(new BigDecimal("10.00")),
                eq(new BigDecimal("100.00")),
                eq(2), eq(0), eq("DESC"), eq("productName")))
                .thenReturn(mockPage);

        mockMvc.perform(get("/categories/{categoryId}/products", validCategoryId)
                        .param("minPrice", "10.00")
                        .param("maxPrice", "100.00")
                        .param("size", "2")
                        .param("page", "0")
                        .param("order", "DESC")
                        .param("sortBy", "productName")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].productName").value("Product One"))
                .andExpect(jsonPath("$.content[0].currentPrice").value("35.0"))
                .andExpect(jsonPath("$.content[1].productName").value("Product Two"))
                .andExpect(jsonPath("$.content[1].currentPrice").value("40.0"))
                .andExpect(jsonPath("$.pageable.pageSize").value(2))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(50))
                .andExpect(jsonPath("$.totalPages").value(25));

        verify(productService, times(1)).getCategoryProducts(
                eq(validCategoryId),
                eq(new BigDecimal("10.00")),
                eq(new BigDecimal("100.00")),
                eq(2), eq(0), eq("DESC"), eq("productName"));
    }

    @Test
    void getCategoryProducts_shouldReturnPagedProducts_whenDefaultParameters() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        ProductResponse productResponse1 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(35.00))
                .productStatus(ProductStatus.AVAILABLE)
                .addedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        ProductResponse productResponse2 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Two")
                .listPrice(BigDecimal.valueOf(50.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(ProductStatus.AVAILABLE)
                .addedAt(Instant.now().minus(11, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .build();

        List<ProductResponse> content = Arrays.asList(productResponse1, productResponse2);
        Page<ProductResponse> mockPage = new PageImpl<>(content, PageRequest.of(0, 10), 20);

        when(productService.getCategoryProducts(
                eq(validCategoryId),
                eq(new BigDecimal("0.0")),
                eq(new BigDecimal("999999.0")),
                eq(10), eq(0), eq("ASC"), eq("addedAt"))).thenReturn(mockPage);

        mockMvc.perform(get("/categories/{categoryId}/products", validCategoryId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].productName").value("Product One"))
                .andExpect(jsonPath("$.content[0].currentPrice").value("35.0"))
                .andExpect(jsonPath("$.content[1].productName").value("Product Two"))
                .andExpect(jsonPath("$.content[1].currentPrice").value("40.0"))
                .andExpect(jsonPath("$.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(20))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(productService, times(1)).getCategoryProducts(
                eq(validCategoryId),
                eq(new BigDecimal("0.0")),
                eq(new BigDecimal("999999.0")),
                eq(10), eq(0), eq("ASC"), eq("addedAt"));
    }

    @Test
    void getCategoryProducts_shouldReturnBadRequest_whenInvalidCategoryId() throws Exception {

        mockMvc.perform(get("/categories/{categoryId}/products", "INVALID_UUID")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getCategoryProducts(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getCategoryProducts_shouldReturnBadRequest_whenMinPriceIsNegative() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        mockMvc.perform(get("/categories/{categoryId}/products", validCategoryId)
                        .param("minPrice", "-0.01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Minimal price must be non-negative")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getCategoryProducts(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getCategoryProducts_shouldReturnBadRequest_whenMaxPriceIsNegative() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        mockMvc.perform(get("/categories/{categoryId}/products", validCategoryId)
                        .param("maxPrice", "-0.01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Maximal price must be non-negative")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getCategoryProducts(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getCategoryProducts_shouldReturnBadRequest_whenMinPriceExceedsLimit() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        mockMvc.perform(get("/categories/{categoryId}/products", validCategoryId)
                        .param("minPrice", "1000000.00")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Minimal price must have up to 6 digits and 2 decimal places","Minimal price must be less than or equal to 999999.99")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getCategoryProducts(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getCategoryProducts_shouldReturnBadRequest_whenMaxPriceExceedsLimit() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        mockMvc.perform(get("/categories/{categoryId}/products", validCategoryId)
                        .param("maxPrice", "1000000.00")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Maximal price must have up to 6 digits and 2 decimal places","Maximal price must be less than or equal to 999999.99")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getCategoryProducts(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getCategoryProducts_shouldReturnBadRequest_whenMinPriceHasTooManyFractionDigits() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        mockMvc.perform(get("/categories/{categoryId}/products", validCategoryId)
                        .param("minPrice", "123456.789")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Minimal price must have up to 6 digits and 2 decimal places")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getCategoryProducts(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getCategoryProducts_shouldReturnBadRequest_whenMaxPriceHasTooManyFractionDigits() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        mockMvc.perform(get("/categories/{categoryId}/products", validCategoryId)
                        .param("maxPrice", "123456.789")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Maximal price must have up to 6 digits and 2 decimal places")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getCategoryProducts(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getCategoryProducts_shouldReturnBadRequest_whenInvalidSize() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        mockMvc.perform(get("/categories/{categoryId}/products", validCategoryId)
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Size must be greater than or equal to 1")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getCategoryProducts(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getCategoryProducts_shouldReturnBadRequest_whenInvalidPage() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        mockMvc.perform(get("/categories/{categoryId}/products", validCategoryId)
                        .param("page", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Page numeration starts from 0")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getCategoryProducts(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getCategoryProducts_shouldReturnBadRequest_whenInvalidOrder() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        mockMvc.perform(get("/categories/{categoryId}/products", validCategoryId)
                        .param("order", "INVALID_ORDER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order: Must be 'ASC' or 'DESC' ('asc' or 'desc')")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getCategoryProducts(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getCategoryProducts_shouldReturnBadRequest_whenInvalidSortBy() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        mockMvc.perform(get("/categories/{categoryId}/products", validCategoryId)
                        .param("sortBy", "INVALID_SORT_BY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid value: Must be one of the following: 'productName', 'listPrice', 'currentPrice', 'addedAt', 'updatedAt'")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getCategoryProducts(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addCategory_shouldReturnCreatedCategory_whenValidRequestAndAdminRole() throws Exception {

        CategoryRequest request = CategoryRequest.builder()
                .categoryName("Test Category")
                .build();

        CategoryResponse expectedResponse = CategoryResponse.builder()
                .categoryId(UUID.randomUUID())
                .categoryName("Test Category")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(categoryService.addCategory(any(CategoryRequest.class))).thenReturn(expectedResponse);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))).andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(expectedResponse.getCategoryId().toString()))
                .andExpect(jsonPath("$.categoryName").value(expectedResponse.getCategoryName()))
                .andExpect(jsonPath("$.categoryStatus").value(expectedResponse.getCategoryStatus().name()))
                .andExpect(jsonPath("$.createdAt").value(expectedResponse.getCreatedAt().toString()))
                .andExpect(jsonPath("$.updatedAt").value(expectedResponse.getUpdatedAt().toString()));

        verify(categoryService, times(1)).addCategory(any(CategoryRequest.class));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addCategory_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        CategoryRequest request = CategoryRequest.builder()
                .categoryName("Test Category")
                .build();

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").value("/categories"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).addCategory(any(CategoryRequest.class));
    }

    @Test
    void addCategory_shouldReturnUnauthorized_whenNoAuthentication() throws Exception {

        CategoryRequest request = CategoryRequest.builder()
                .categoryName("Test Category")
                .build();

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/categories"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, times(0)).addCategory(any(CategoryRequest.class));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addCategory_shouldReturnBadRequest_whenCategoryNameIsBlank() throws Exception {

        CategoryRequest invalidRequest = CategoryRequest.builder()
                .categoryName("")
                .build();

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Category name is required","Invalid category name: Must be of 2 - 50 characters")))
                .andExpect(jsonPath("$.path").value("/categories"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).addCategory(any(CategoryRequest.class));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addCategory_shouldReturnBadRequest_whenCategoryNameIsTooShort() throws Exception {

        CategoryRequest invalidRequest = CategoryRequest.builder()
                .categoryName("A")
                .build();

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid category name: Must be of 2 - 50 characters")))
                .andExpect(jsonPath("$.path").value("/categories"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).addCategory(any(CategoryRequest.class));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addCategory_shouldReturnBadRequest_whenCategoryNameIsTooLong() throws Exception {

        CategoryRequest invalidRequest = CategoryRequest.builder()
                .categoryName("A".repeat(51))
                .build();

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid category name: Must be of 2 - 50 characters")))
                .andExpect(jsonPath("$.path").value("/categories"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).addCategory(any(CategoryRequest.class));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateCategory_shouldReturnUpdatedCategory_whenValidRequestAndAdminRole() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        CategoryRequest request = CategoryRequest.builder()
                .categoryName("Updated Category Name")
                .build();

        CategoryResponse expectedResponse = CategoryResponse.builder()
                .categoryId(UUID.randomUUID())
                .categoryName("Test Category")
                .categoryStatus(CategoryStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(categoryService.updateCategory(eq(validCategoryId), eq(request)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/categories/{categoryId}", validCategoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(expectedResponse.getCategoryId().toString()))
                .andExpect(jsonPath("$.categoryName").value(expectedResponse.getCategoryName()))
                .andExpect(jsonPath("$.categoryStatus").value(expectedResponse.getCategoryStatus().name()))
                .andExpect(jsonPath("$.createdAt").value(expectedResponse.getCreatedAt().toString()))
                .andExpect(jsonPath("$.updatedAt").value(expectedResponse.getUpdatedAt().toString()));

        verify(categoryService, times(1)).updateCategory(eq(validCategoryId), eq(request));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateCategory_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        CategoryRequest request = CategoryRequest.builder()
                .categoryName("Updated Category Name")
                .build();


        mockMvc.perform(patch("/categories/{categoryId}", validCategoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).updateCategory(any(), any());
    }

    @Test
    void updateCategory_shouldReturnUnauthorized_whenNoAuthentication() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        CategoryRequest request = CategoryRequest.builder()
                .categoryName("Updated Category Name")
                .build();

        mockMvc.perform(patch("/categories/{categoryId}", validCategoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).updateCategory(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateCategory_shouldReturnBadRequest_whenInvalidCategoryId() throws Exception {

        CategoryRequest request = CategoryRequest.builder()
                .categoryName("Updated Category Name")
                .build();

        mockMvc.perform(patch("/categories/{categoryId}", "INVALID_UUID")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).updateCategory(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateCategory_shouldReturnBadRequest_whenCategoryNameIsBlank() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        CategoryRequest invalidRequest = CategoryRequest.builder()
                .categoryName("")
                .build();

        mockMvc.perform(patch("/categories/{categoryId}", validCategoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Category name is required","Invalid category name: Must be of 2 - 50 characters")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).updateCategory(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateCategory_shouldReturnBadRequest_whenCategoryNameIsTooShort() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        CategoryRequest invalidRequest = CategoryRequest.builder()
                .categoryName("A")
                .build();

        mockMvc.perform(patch("/categories/{categoryId}", validCategoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid category name: Must be of 2 - 50 characters")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).updateCategory(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateCategory_shouldReturnBadRequest_whenCategoryNameIsTooLong() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        CategoryRequest invalidRequest = CategoryRequest.builder()
                .categoryName("A".repeat(51))
                .build();

        mockMvc.perform(patch("/categories/{categoryId}", validCategoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid category name: Must be of 2 - 50 characters")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).updateCategory(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void setCategoryStatus_shouldReturnOk_whenValidRequestAndAdminRole() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        MessageResponse expectedResponse = MessageResponse.builder()
                .message("Category status updated successfully to ACTIVE")
                .build();

        when(categoryService.setCategoryStatus(eq(validCategoryId), eq("ACTIVE")))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/categories/{categoryId}/status", validCategoryId)
                        .param("categoryStatus", "ACTIVE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(expectedResponse.getMessage()));

        verify(categoryService, times(1)).setCategoryStatus(eq(validCategoryId), eq("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void setCategoryStatus_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        mockMvc.perform(patch("/categories/{categoryId}/status", validCategoryId)
                        .param("categoryStatus", "ACTIVE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).setCategoryStatus(any(), any());
    }

    @Test
    void setCategoryStatus_shouldReturnUnauthorized_whenNoAuthentication() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        mockMvc.perform(patch("/categories/{categoryId}/status", validCategoryId)
                        .param("categoryStatus", "ACTIVE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).setCategoryStatus(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void setCategoryStatus_shouldReturnOk_whenStatusOmittedAndDefaultUsed() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        MessageResponse expectedResponse = MessageResponse.builder()
                .message("Category status updated successfully to ACTIVE")
                .build();
        when(categoryService.setCategoryStatus(eq(validCategoryId), eq("ACTIVE")))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/categories/{categoryId}/status", validCategoryId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(expectedResponse.getMessage()));

        verify(categoryService, times(1)).setCategoryStatus(eq(validCategoryId), eq("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void setCategoryStatus_shouldReturnBadRequest_whenInvalidCategoryId() throws Exception {

        mockMvc.perform(patch("/categories/{categoryId}/status", "INVALID_UUID")
                        .param("categoryStatus", "ACTIVE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).setCategoryStatus(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void setCategoryStatus_shouldReturnBadRequest_whenInvalidCategoryStatus() throws Exception {

        String validCategoryId = UUID.randomUUID().toString();

        mockMvc.perform(patch("/categories/{categoryId}/status", validCategoryId)
                        .param("categoryStatus", "INVALID_STATUS")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order orderStatus: Must be one of the: 'ACTIVE' or 'INACTIVE' ('active' or 'inactive')")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(categoryService, never()).setCategoryStatus(any(), any());
    }
}