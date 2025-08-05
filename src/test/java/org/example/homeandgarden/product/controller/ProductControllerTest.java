package org.example.homeandgarden.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.homeandgarden.product.dto.*;
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
import org.springframework.data.domain.Sort;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ProductControllerTest.TestConfig.class)
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {

        @Bean @Primary
        public ProductServiceImpl productService() {
            return mock(ProductServiceImpl.class);
        }
    }

    @Autowired
    private ProductServiceImpl productService;

    @AfterEach
    void resetMocks() {
        reset(productService);
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getProductsByStatus_shouldReturnPagedProducts_whenValidParametersAndAdminRole() throws Exception {

        ProductResponse productResponse1 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product One")
                .description("Description One")
                .listPrice(BigDecimal.valueOf(40.0))
                .currentPrice(BigDecimal.valueOf(35.0))
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl("https://example.com/image1.jpg")
                .addedAt(Instant.now().minus(11, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        ProductResponse productResponse2 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Two")
                .description("Description Two")
                .listPrice(BigDecimal.valueOf(50.0))
                .currentPrice(BigDecimal.valueOf(40.0))
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl("https://example.com/image2.jpg")
                .addedAt(Instant.now().minus(8, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .build();

        List<ProductResponse> content = Arrays.asList(productResponse1, productResponse2);
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.Direction.DESC, "productName");
        Page<ProductResponse> mockPage = new PageImpl<>(content, pageRequest, 50);

        when(productService.getProductsByStatus(eq("AVAILABLE"), eq(2), eq(0), eq("DESC"), eq("productName")))
                .thenReturn(mockPage);

        mockMvc.perform(get("/products/status")
                        .param("productStatus", "AVAILABLE")
                        .param("size", "2")
                        .param("page", "0")
                        .param("order", "DESC")
                        .param("sortBy", "productName")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].productName").value("Product One"))
                .andExpect(jsonPath("$.content[0].description").value("Description One"))
                .andExpect(jsonPath("$.content[0].listPrice").value(BigDecimal.valueOf(40.0)))
                .andExpect(jsonPath("$.content[0].currentPrice").value(BigDecimal.valueOf(35.0)))
                .andExpect(jsonPath("$.content[0].productStatus").value(ProductStatus.AVAILABLE.name()))
                .andExpect(jsonPath("$.content[0].imageUrl").value("https://example.com/image1.jpg"))

                .andExpect(jsonPath("$.content[1].productName").value("Product Two"))
                .andExpect(jsonPath("$.content[1].description").value("Description Two"))
                .andExpect(jsonPath("$.content[1].listPrice").value(BigDecimal.valueOf(50.0)))
                .andExpect(jsonPath("$.content[1].currentPrice").value(BigDecimal.valueOf(40.0)))
                .andExpect(jsonPath("$.content[1].productStatus").value(ProductStatus.AVAILABLE.name()))
                .andExpect(jsonPath("$.content[1].imageUrl").value("https://example.com/image2.jpg"))

                .andExpect(jsonPath("$.pageable.pageSize").value(2))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(50))
                .andExpect(jsonPath("$.totalPages").value(25));

        verify(productService, times(1)).getProductsByStatus(eq("AVAILABLE"), eq(2), eq(0), eq("DESC"), eq("productName"));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getProductsByStatus_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        mockMvc.perform(get("/products/status")
                        .param("productStatus", "AVAILABLE")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").value("/products/status"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getProductsByStatus(any(), any(), any(), any(), any());
    }

    @Test
    void getProductsByStatus_shouldReturnUnauthorized_whenNoAuthentication() throws Exception {

        mockMvc.perform(get("/products/status")
                        .param("productStatus", "AVAILABLE")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/products/status"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getProductsByStatus(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getProductsByStatus_shouldReturnPagedProducts_whenStatusOmittedAndDefaultParameters() throws Exception {

        ProductResponse productResponse1 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product One")
                .description("Description One")
                .listPrice(BigDecimal.valueOf(40.0))
                .currentPrice(BigDecimal.valueOf(35.0))
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl("https://example.com/image1.jpg")
                .addedAt(Instant.now().minus(11, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        ProductResponse productResponse2 = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Product Two")
                .description("Description Two")
                .listPrice(BigDecimal.valueOf(50.0))
                .currentPrice(BigDecimal.valueOf(40.0))
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl("https://example.com/image2.jpg")
                .addedAt(Instant.now().minus(8, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .build();

        List<ProductResponse> content = Arrays.asList(productResponse1, productResponse2);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.Direction.ASC, "addedAt");
        Page<ProductResponse> mockPage = new PageImpl<>(content, pageRequest, 100);

        when(productService.getProductsByStatus(eq(null), eq(10), eq(0), eq("ASC"), eq("addedAt")))
                .thenReturn(mockPage);

        mockMvc.perform(get("/products/status")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].productName").value("Product One"))
                .andExpect(jsonPath("$.content[0].description").value("Description One"))
                .andExpect(jsonPath("$.content[0].listPrice").value(BigDecimal.valueOf(40.0)))
                .andExpect(jsonPath("$.content[0].currentPrice").value(BigDecimal.valueOf(35.0)))
                .andExpect(jsonPath("$.content[0].productStatus").value(ProductStatus.AVAILABLE.name()))
                .andExpect(jsonPath("$.content[0].imageUrl").value("https://example.com/image1.jpg"))

                .andExpect(jsonPath("$.content[1].productName").value("Product Two"))
                .andExpect(jsonPath("$.content[1].description").value("Description Two"))
                .andExpect(jsonPath("$.content[1].listPrice").value(BigDecimal.valueOf(50.0)))
                .andExpect(jsonPath("$.content[1].currentPrice").value(BigDecimal.valueOf(40.0)))
                .andExpect(jsonPath("$.content[1].productStatus").value(ProductStatus.AVAILABLE.name()))
                .andExpect(jsonPath("$.content[1].imageUrl").value("https://example.com/image2.jpg"))

                .andExpect(jsonPath("$.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(100))
                .andExpect(jsonPath("$.totalPages").value(10));

        verify(productService, times(1)).getProductsByStatus(eq(null), eq(10), eq(0), eq("ASC"), eq("addedAt"));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getProductsByStatus_shouldReturnBadRequest_whenInvalidProductStatus() throws Exception {

        mockMvc.perform(get("/products/status")
                        .param("productStatus", "INVALID_STATUS")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order orderStatus: Must be one of the: 'AVAILABLE', 'OUT_OF_STOCK' or 'SOLD_OUT' ('available', 'out_of_stock' or 'sold_out')")))
                .andExpect(jsonPath("$.path").value("/products/status"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getProductsByStatus(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getProductsByStatus_shouldReturnBadRequest_whenInvalidSize() throws Exception {

        mockMvc.perform(get("/products/status")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Size must be greater than or equal to 1")))
                .andExpect(jsonPath("$.path").value("/products/status"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getProductsByStatus(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getProductsByStatus_shouldReturnBadRequest_whenInvalidPage() throws Exception {

        mockMvc.perform(get("/products/status")
                        .param("page", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Page numeration starts from 0")))
                .andExpect(jsonPath("$.path").value("/products/status"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getProductsByStatus(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getProductsByStatus_shouldReturnBadRequest_whenInvalidOrder() throws Exception {

        mockMvc.perform(get("/products/status")
                        .param("order", "INVALID_ORDER")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order: Must be 'ASC' or 'DESC' ('asc' or 'desc')")))
                .andExpect(jsonPath("$.path").value("/products/status"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getProductsByStatus(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getProductsByStatus_shouldReturnBadRequest_whenInvalidSortBy() throws Exception {

        mockMvc.perform(get("/products/status")
                        .param("sortBy", "INVALID_SORT_BY")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid value: Must be one of the following: 'productName', 'listPrice', 'currentPrice', 'addedAt', 'updatedAt'")))
                .andExpect(jsonPath("$.path").value("/products/status"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getProductsByStatus(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getTopProducts_shouldReturnPagedProductProjections_whenValidParametersAndAdminRole() throws Exception {

        ProductProjectionResponse productProjection1 = ProductProjectionResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Projection One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl("https://example.com/image1.jpg")
                .addedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .totalAmount(56L)
                .build();

        ProductProjectionResponse productProjection2 = ProductProjectionResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Projection Two")
                .listPrice(BigDecimal.valueOf(30.00))
                .currentPrice(BigDecimal.valueOf(20.00))
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl("https://example.com/image2.jpg")
                .addedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .totalAmount(39L)
                .build();

        List<ProductProjectionResponse> content = Arrays.asList(productProjection1, productProjection2);
        Page<ProductProjectionResponse> mockPage = new PageImpl<>(content, PageRequest.of(0, 2), 10);

        when(productService.getTopProducts(eq("CANCELED"), eq(2), eq(0))).thenReturn(mockPage);

        mockMvc.perform(get("/products/top")
                        .param("status", "CANCELED")
                        .param("size", "2")
                        .param("page", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].productName").value("Projection One"))
                .andExpect(jsonPath("$.content[0].listPrice").value(BigDecimal.valueOf(40.0)))
                .andExpect(jsonPath("$.content[0].currentPrice").value(BigDecimal.valueOf(40.0)))
                .andExpect(jsonPath("$.content[0].productStatus").value(ProductStatus.AVAILABLE.name()))
                .andExpect(jsonPath("$.content[0].imageUrl").value("https://example.com/image1.jpg"))

                .andExpect(jsonPath("$.content[1].productName").value("Projection Two"))
                .andExpect(jsonPath("$.content[1].listPrice").value(BigDecimal.valueOf(30.0)))
                .andExpect(jsonPath("$.content[1].currentPrice").value(BigDecimal.valueOf(20.0)))
                .andExpect(jsonPath("$.content[1].productStatus").value(ProductStatus.AVAILABLE.name()))
                .andExpect(jsonPath("$.content[1].imageUrl").value("https://example.com/image2.jpg"))

                .andExpect(jsonPath("$.pageable.pageSize").value(2))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(5));

        verify(productService, times(1)).getTopProducts(eq("CANCELED"), eq(2), eq(0));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getTopProducts_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        mockMvc.perform(get("/products/top")
                        .param("status", "PAID")
                        .param("status", "PAID")
                        .param("size", "2")
                        .param("page", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").value("/products/top"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getTopProducts(any(), any(), any());
    }

    @Test
    void getTopProducts_shouldReturnUnauthorized_whenNoAuthentication() throws Exception {

        mockMvc.perform(get("/products/top")
                        .param("status", "PAID")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/products/top"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getTopProducts(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getTopProducts_shouldReturnPagedProductProjections_whenDefaultParameters() throws Exception {

        ProductProjectionResponse productProjection1 = ProductProjectionResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Projection One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl("https://example.com/image1.jpg")
                .addedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .totalAmount(56L)
                .build();

        ProductProjectionResponse productProjection2 = ProductProjectionResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Projection Two")
                .listPrice(BigDecimal.valueOf(30.00))
                .currentPrice(BigDecimal.valueOf(20.00))
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl("https://example.com/image2.jpg")
                .addedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .totalAmount(39L)
                .build();

        List<ProductProjectionResponse> content = Arrays.asList(productProjection1, productProjection2);
        Page<ProductProjectionResponse> mockPage = new PageImpl<>(content, PageRequest.of(0, 10), 20);

        when(productService.getTopProducts(eq("PAID"), eq(10), eq(0))).thenReturn(mockPage);

        mockMvc.perform(get("/products/top")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].productName").value("Projection One"))
                .andExpect(jsonPath("$.content[0].listPrice").value(BigDecimal.valueOf(40.0)))
                .andExpect(jsonPath("$.content[0].currentPrice").value(BigDecimal.valueOf(40.0)))
                .andExpect(jsonPath("$.content[0].productStatus").value(ProductStatus.AVAILABLE.name()))
                .andExpect(jsonPath("$.content[0].imageUrl").value("https://example.com/image1.jpg"))

                .andExpect(jsonPath("$.content[1].productName").value("Projection Two"))
                .andExpect(jsonPath("$.content[1].listPrice").value(BigDecimal.valueOf(30.0)))
                .andExpect(jsonPath("$.content[1].currentPrice").value(BigDecimal.valueOf(20.0)))
                .andExpect(jsonPath("$.content[1].productStatus").value(ProductStatus.AVAILABLE.name()))
                .andExpect(jsonPath("$.content[1].imageUrl").value("https://example.com/image2.jpg"))

                .andExpect(jsonPath("$.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(20))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(productService, times(1)).getTopProducts(eq("PAID"), eq(10), eq(0));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getTopProducts_shouldReturnBadRequest_whenInvalidStatus() throws Exception {
        mockMvc.perform(get("/products/top")
                        .param("status", "INVALID_STATUS")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid status: Must be 'PAID' or 'CANCELED' ('paid' or 'canceled')")))
                .andExpect(jsonPath("$.path").value("/products/top"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getTopProducts(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getTopProducts_shouldReturnBadRequest_whenInvalidSize() throws Exception {
        mockMvc.perform(get("/products/top")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Size must be greater than or equal to 1")))
                .andExpect(jsonPath("$.path").value("/products/top"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getTopProducts(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getTopProducts_shouldReturnBadRequest_whenInvalidPage() throws Exception {
        mockMvc.perform(get("/products/top")
                        .param("page", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Page numeration starts from 0")))
                .andExpect(jsonPath("$.path").value("/products/top"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getTopProducts(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getPendingProducts_shouldReturnPagedProductProjections_whenValidParametersAndAdminRole() throws Exception {

        ProductProjectionResponse productProjection1 = ProductProjectionResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Projection One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl("https://example.com/image1.jpg")
                .addedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .totalAmount(56L)
                .build();

        ProductProjectionResponse productProjection2 = ProductProjectionResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Projection Two")
                .listPrice(BigDecimal.valueOf(30.00))
                .currentPrice(BigDecimal.valueOf(20.00))
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl("https://example.com/image2.jpg")
                .addedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .totalAmount(39L)
                .build();

        List<ProductProjectionResponse> content = Arrays.asList(productProjection1, productProjection2);
        Page<ProductProjectionResponse> mockPage = new PageImpl<>(content, PageRequest.of(0, 2), 10);

        when(productService.getPendingProducts(eq("PAID"), eq(7), eq(2), eq(0)))
                .thenReturn(mockPage);

        mockMvc.perform(get("/products/pending")
                        .param("orderStatus", "PAID")
                        .param("days", "7")
                        .param("size", "2")
                        .param("page", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].productName").value("Projection One"))
                .andExpect(jsonPath("$.content[0].listPrice").value(BigDecimal.valueOf(40.0)))
                .andExpect(jsonPath("$.content[0].currentPrice").value(BigDecimal.valueOf(40.0)))
                .andExpect(jsonPath("$.content[0].productStatus").value(ProductStatus.AVAILABLE.name()))
                .andExpect(jsonPath("$.content[0].imageUrl").value("https://example.com/image1.jpg"))

                .andExpect(jsonPath("$.content[1].productName").value("Projection Two"))
                .andExpect(jsonPath("$.content[1].listPrice").value(BigDecimal.valueOf(30.0)))
                .andExpect(jsonPath("$.content[1].currentPrice").value(BigDecimal.valueOf(20.0)))
                .andExpect(jsonPath("$.content[1].productStatus").value(ProductStatus.AVAILABLE.name()))
                .andExpect(jsonPath("$.content[1].imageUrl").value("https://example.com/image2.jpg"))

                .andExpect(jsonPath("$.pageable.pageSize").value(2))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(5));

        verify(productService, times(1)).getPendingProducts(eq("PAID"), eq(7), eq(2), eq(0));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getPendingProducts_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        mockMvc.perform(get("/products/pending")
                        .param("orderStatus", "PAID")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").value("/products/pending"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getPendingProducts(any(), any(), any(), any());
    }

    @Test
    void getPendingProducts_shouldReturnUnauthorized_whenNoAuthentication() throws Exception {

        mockMvc.perform(get("/products/pending")
                        .param("orderStatus", "PAID")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/products/pending"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getPendingProducts(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getPendingProducts_shouldReturnPagedProductProjections_whenDefaultParameters() throws Exception {

        ProductProjectionResponse productProjection1 = ProductProjectionResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Projection One")
                .listPrice(BigDecimal.valueOf(40.00))
                .currentPrice(BigDecimal.valueOf(40.00))
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl("https://example.com/image1.jpg")
                .addedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .totalAmount(56L)
                .build();

        ProductProjectionResponse productProjection2 = ProductProjectionResponse.builder()
                .productId(UUID.randomUUID())
                .productName("Projection Two")
                .listPrice(BigDecimal.valueOf(30.00))
                .currentPrice(BigDecimal.valueOf(20.00))
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl("https://example.com/image2.jpg")
                .addedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .totalAmount(39L)
                .build();


        List<ProductProjectionResponse> content = Arrays.asList(productProjection1, productProjection2);
        Page<ProductProjectionResponse> mockPage = new PageImpl<>(content, PageRequest.of(0, 10), 20);

        when(productService.getPendingProducts(eq("CREATED"), eq(10), eq(10), eq(0))).thenReturn(mockPage);

        mockMvc.perform(get("/products/pending")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content[0].productName").value("Projection One"))
                .andExpect(jsonPath("$.content[0].listPrice").value(BigDecimal.valueOf(40.0)))
                .andExpect(jsonPath("$.content[0].currentPrice").value(BigDecimal.valueOf(40.0)))
                .andExpect(jsonPath("$.content[0].productStatus").value(ProductStatus.AVAILABLE.name()))
                .andExpect(jsonPath("$.content[0].imageUrl").value("https://example.com/image1.jpg"))

                .andExpect(jsonPath("$.content[1].productName").value("Projection Two"))
                .andExpect(jsonPath("$.content[1].listPrice").value(BigDecimal.valueOf(30.0)))
                .andExpect(jsonPath("$.content[1].currentPrice").value(BigDecimal.valueOf(20.0)))
                .andExpect(jsonPath("$.content[1].productStatus").value(ProductStatus.AVAILABLE.name()))
                .andExpect(jsonPath("$.content[1].imageUrl").value("https://example.com/image2.jpg"))

                .andExpect(jsonPath("$.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(20))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(productService, times(1)).getPendingProducts(eq("CREATED"), eq(10), eq(10), eq(0));
    }


    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getPendingProducts_shouldReturnBadRequest_whenInvalidOrderStatus() throws Exception {
        mockMvc.perform(get("/products/pending")
                        .param("orderStatus", "INVALID_STATUS")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order orderStatus: Must be 'CREATED', 'PAID' or 'ON_THE_WAY' ('created', 'paid' or 'on_the_way')")))
                .andExpect(jsonPath("$.path").value("/products/pending"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getPendingProducts(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getPendingProducts_shouldReturnBadRequest_whenNegativeDays() throws Exception {
        mockMvc.perform(get("/products/pending")
                        .param("days", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Number of days must be a positive number")))
                .andExpect(jsonPath("$.path").value("/products/pending"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getPendingProducts(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getPendingProducts_shouldReturnBadRequest_whenInvalidSize() throws Exception {
        mockMvc.perform(get("/products/pending")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Size must be greater than or equal to 1")))
                .andExpect(jsonPath("$.path").value("/products/pending"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getPendingProducts(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getPendingProducts_shouldReturnBadRequest_whenInvalidPage() throws Exception {
        mockMvc.perform(get("/products/pending")
                        .param("page", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid parameter: Page numeration starts from 0")))
                .andExpect(jsonPath("$.path").value("/products/pending"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getPendingProducts(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getProfitByPeriod_shouldReturnProductProfitResponse_whenValidParametersAndAdminRole() throws Exception {

        ProductProfitResponse expectedResponse = ProductProfitResponse.builder()
                .timeUnit("WEEK")
                .timePeriod(4)
                .profit(BigDecimal.valueOf(12345.67))
                .build();

        when(productService.getProfitByPeriod(eq("WEEK"), eq(4))).thenReturn(expectedResponse);

        mockMvc.perform(get("/products/profit")
                        .param("timeUnit", "WEEK")
                        .param("timePeriod", "4")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profit").value(BigDecimal.valueOf(12345.67)))
                .andExpect(jsonPath("$.timeUnit").value("WEEK"))
                .andExpect(jsonPath("$.timePeriod").value(4));

        verify(productService, times(1)).getProfitByPeriod(eq("WEEK"), eq(4));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void getProfitByPeriod_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        mockMvc.perform(get("/products/profit")
                        .param("timeUnit", "DAY")
                        .param("timePeriod", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").value("/products/profit"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getProfitByPeriod(any(), any());
    }

    @Test
    void getProfitByPeriod_shouldReturnUnauthorized_whenNoAuthentication() throws Exception {

        mockMvc.perform(get("/products/profit")
                        .param("timeUnit", "DAY")
                        .param("timePeriod", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/products/profit"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getProfitByPeriod(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getProfitByPeriod_shouldReturnProductProfitResponse_whenDefaultParameters() throws Exception {

        ProductProfitResponse expectedResponse = ProductProfitResponse.builder()
                .timeUnit("DAY")
                .timePeriod(10)
                .profit(BigDecimal.valueOf(5000.00))
                .build();

        when(productService.getProfitByPeriod(eq("DAY"), eq(10))).thenReturn(expectedResponse);

        mockMvc.perform(get("/products/profit").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profit").value(BigDecimal.valueOf(5000.00)))
                .andExpect(jsonPath("$.timeUnit").value("DAY"))
                .andExpect(jsonPath("$.timePeriod").value(10));

        verify(productService, times(1)).getProfitByPeriod(eq("DAY"), eq(10));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getProfitByPeriod_shouldReturnBadRequest_whenInvalidTimeUnit() throws Exception {
        mockMvc.perform(get("/products/profit")
                        .param("timeUnit", "INVALID_UNIT")
                        .param("timePeriod", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid type of period: Must be 'DAY', 'WEEK', 'MONTH' or 'YEAR' ('day', 'week', 'month' or 'year')")))
                .andExpect(jsonPath("$.path").value("/products/profit"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getProfitByPeriod(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void getProfitByPeriod_shouldReturnBadRequest_whenNonPositiveTimePeriod() throws Exception {

        mockMvc.perform(get("/products/profit")
                        .param("timeUnit", "DAY")
                        .param("timePeriod", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Duration must be a positive number")))
                .andExpect(jsonPath("$.path").value("/products/profit"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getProfitByPeriod(any(), any());
    }

    @Test
    void getProductById_shouldReturnProduct_whenValidId() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductResponse expectedProduct = ProductResponse.builder()
                .productId(UUID.fromString(validProductId))
                .productName("Product Name")
                .description("Product Description")
                .listPrice(BigDecimal.valueOf(100.00))
                .currentPrice(BigDecimal.valueOf(90.00))
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl("http://example.com/image.jpg")
                .addedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        when(productService.getProductById(eq(validProductId))).thenReturn(expectedProduct);

        mockMvc.perform(get("/products/{productId}", validProductId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(validProductId))
                .andExpect(jsonPath("$.productName").value("Product Name"))
                .andExpect(jsonPath("$.description").value("Product Description"))
                .andExpect(jsonPath("$.listPrice").value(BigDecimal.valueOf(100.00)))
                .andExpect(jsonPath("$.currentPrice").value(BigDecimal.valueOf(90.00)))
                .andExpect(jsonPath("$.productStatus").value(ProductStatus.AVAILABLE.name()))
                .andExpect(jsonPath("$.imageUrl").value("http://example.com/image.jpg"))
                .andExpect(jsonPath("$.addedAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(productService, times(1)).getProductById(eq(validProductId));
    }

    @Test
    void getProductById_shouldReturnBadRequest_whenInvalidProductIdFormat() throws Exception {

        String invalidProductId = "INVALID_UUID";

        mockMvc.perform(get("/products/{productId}", invalidProductId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).getProductById(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addProduct_shouldReturnCreatedProduct_whenValidRequestAndAdminRole() throws Exception {

        String categoryId = UUID.randomUUID().toString();

        ProductCreateRequest createRequest = ProductCreateRequest.builder()
                .categoryId(categoryId)
                .productName("New Name")
                .description("New Description")
                .listPrice(BigDecimal.valueOf(199.99))
                .imageUrl("http://example.com/image.jpg")
                .build();

        ProductResponse expectedResponse = ProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName(createRequest.getProductName())
                .description(createRequest.getDescription())
                .listPrice(createRequest.getListPrice())
                .currentPrice(createRequest.getListPrice())
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl(createRequest.getImageUrl())
                .addedAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(productService.addProduct(eq(createRequest))).thenReturn(expectedResponse);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").exists())
                .andExpect(jsonPath("$.productName").value("New Name"))
                .andExpect(jsonPath("$.description").value("New Description"))
                .andExpect(jsonPath("$.listPrice").value(BigDecimal.valueOf(199.99)))
                .andExpect(jsonPath("$.currentPrice").value(BigDecimal.valueOf(199.99)))
                .andExpect(jsonPath("$.imageUrl").value("http://example.com/image.jpg"))
                .andExpect(jsonPath("$.productStatus").value(ProductStatus.AVAILABLE.name()));

        verify(productService, times(1)).addProduct(eq(createRequest));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void addProduct_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        ProductCreateRequest createRequest = ProductCreateRequest.builder()
                .categoryId(UUID.randomUUID().toString())
                .productName("New Name")
                .description("New Description")
                .listPrice(BigDecimal.valueOf(199.99))
                .imageUrl("http://example.com/image.jpg")
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").value("/products"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).addProduct(any());
    }

    @Test
    void addProduct_shouldReturnUnauthorized_whenNoAuthentication() throws Exception {

        ProductCreateRequest createRequest = ProductCreateRequest.builder()
                .categoryId(UUID.randomUUID().toString())
                .productName("New Name")
                .description("New Description")
                .listPrice(BigDecimal.valueOf(199.99))
                .imageUrl("http://example.com/image.jpg")
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/products"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).addProduct(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addProduct_shouldReturnBadRequest_whenCategoryIdIsAnEmptyString() throws Exception {

        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .categoryId("")
                .productName("Valid Name")
                .description("Valid Description")
                .listPrice(BigDecimal.TEN)
                .imageUrl("https://valid.com/img.jpg")
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format","Category id is required")))
                .andExpect(jsonPath("$.path").value("/products"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).addProduct(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addProduct_shouldReturnBadRequest_whenInvalidCategoryId() throws Exception {

        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .categoryId("INVALID_UUID")
                .productName("Valid Name")
                .description("Valid Description")
                .listPrice(BigDecimal.TEN)
                .imageUrl("https://valid.com/img.jpg")
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").value("/products"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).addProduct(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addProduct_shouldReturnBadRequest_whenProductNameIsBlank() throws Exception {

        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .categoryId(UUID.randomUUID().toString())
                .productName("")
                .description("Valid Description")
                .listPrice(BigDecimal.TEN)
                .imageUrl("https://valid.com/img.jpg")
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Product name is required",
                        "Invalid name: Must be of 2 - 50 characters")))
                .andExpect(jsonPath("$.path").value("/products"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).addProduct(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addProduct_shouldReturnBadRequest_whenProductNameIsTooShort() throws Exception {

        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .categoryId(UUID.randomUUID().toString())
                .productName("A")
                .description("Valid Description")
                .listPrice(BigDecimal.TEN)
                .imageUrl("https://valid.com/img.jpg")
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid name: Must be of 2 - 50 characters")))
                .andExpect(jsonPath("$.path").value("/products"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).addProduct(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addProduct_shouldReturnBadRequest_whenProductNameIsTooLong() throws Exception {

        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .categoryId(UUID.randomUUID().toString())
                .productName("A".repeat(51))
                .description("Valid Description")
                .listPrice(BigDecimal.TEN)
                .imageUrl("https://valid.com/img.jpg")
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder(                        "Invalid name: Must be of 2 - 50 characters")))
                .andExpect(jsonPath("$.path").value("/products"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).addProduct(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addProduct_shouldReturnBadRequest_whenDescriptionIsBlank() throws Exception {

        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .categoryId(UUID.randomUUID().toString())
                .productName("Valid Name")
                .description("")
                .listPrice(BigDecimal.TEN)
                .imageUrl("https://valid.com/img.jpg")
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Description is required", "Invalid description: Must be of 2 - 255 characters")))
                .andExpect(jsonPath("$.path").value("/products"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).addProduct(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addProduct_shouldReturnBadRequest_whenDescriptionIsTooShort() throws Exception {

        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .categoryId(UUID.randomUUID().toString())
                .productName("Valid Name")
                .description("A")
                .listPrice(BigDecimal.TEN)
                .imageUrl("https://valid.com/img.jpg")
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid description: Must be of 2 - 255 characters")))
                .andExpect(jsonPath("$.path").value("/products"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).addProduct(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addProduct_shouldReturnBadRequest_whenDescriptionIsTooLong() throws Exception {

        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .categoryId(UUID.randomUUID().toString())
                .productName("Valid Name")
                .description("A".repeat(256))
                .listPrice(BigDecimal.TEN)
                .imageUrl("https://valid.com/img.jpg")
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid description: Must be of 2 - 255 characters")))
                .andExpect(jsonPath("$.path").value("/products"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).addProduct(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addProduct_shouldReturnBadRequest_whenListPriceIsNull() throws Exception {

        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .categoryId(UUID.randomUUID().toString())
                .productName("Valid Name")
                .description("Valid Description")
                .listPrice(null)
                .imageUrl("https://valid.com/img.jpg")
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("List price is required")))
                .andExpect(jsonPath("$.path").value("/products"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).addProduct(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addProduct_shouldReturnBadRequest_whenListPriceIsNegative() throws Exception {

        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .categoryId(UUID.randomUUID().toString())
                .productName("Valid Name")
                .description("Valid Description")
                .listPrice(BigDecimal.valueOf(-0.01))
                .imageUrl("https://valid.com/img.jpg")
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("List price must be non-negative")))
                .andExpect(jsonPath("$.path").value("/products"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).addProduct(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addProduct_shouldReturnBadRequest_whenListPriceExceedsLimit() throws Exception {

        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .categoryId(UUID.randomUUID().toString())
                .productName("Valid Name")
                .description("Valid Description")
                .listPrice(BigDecimal.valueOf(1000000.00))
                .imageUrl("https://valid.com/img.jpg")
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("List price must have up to 6 digits and 2 decimal places","List price must be less than or equal to 999999.99")))
                .andExpect(jsonPath("$.path").value("/products"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).addProduct(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addProduct_shouldReturnBadRequest_whenListPriceHasTooManyFractionDigits() throws Exception {

        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .categoryId(UUID.randomUUID().toString())
                .productName("Valid Name")
                .description("Valid Description")
                .listPrice(BigDecimal.valueOf(123456.789))
                .imageUrl("https://valid.com/img.jpg")
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("List price must have up to 6 digits and 2 decimal places")))
                .andExpect(jsonPath("$.path").value("/products"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).addProduct(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addProduct_shouldReturnBadRequest_whenImageUrlIsBlank() throws Exception {

        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .categoryId(UUID.randomUUID().toString())
                .productName("Valid Name")
                .description("Valid Description")
                .listPrice(BigDecimal.TEN)
                .imageUrl("")
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Image url is required", "Invalid URL")))
                .andExpect(jsonPath("$.path").value("/products"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).addProduct(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void addProduct_shouldReturnBadRequest_whenInvalidImageUrl() throws Exception {

        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .categoryId(UUID.randomUUID().toString())
                .productName("Valid Name")
                .description("Valid Description")
                .listPrice(BigDecimal.TEN)
                .imageUrl("INVALID_URL")
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid URL")))
                .andExpect(jsonPath("$.path").value("/products"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).addProduct(any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnUpdatedProduct_whenValidRequestAndAdminRole() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .productName("Updated Product Name")
                .description("Updated Description")
                .listPrice(BigDecimal.valueOf(250.00))
                .currentPrice(BigDecimal.valueOf(220.00))
                .imageUrl("https://example.com/updated-image.jpg")
                .build();

        ProductResponse expectedResponse = ProductResponse.builder()
                .productId(UUID.fromString(validProductId))
                .productName(updateRequest.getProductName())
                .description(updateRequest.getDescription())
                .listPrice(updateRequest.getListPrice())
                .currentPrice(updateRequest.getCurrentPrice())
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl(updateRequest.getImageUrl())
                .addedAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(productService.updateProduct(eq(validProductId), eq(updateRequest))).thenReturn(expectedResponse);

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(validProductId))
                .andExpect(jsonPath("$.productName").value("Updated Product Name"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.listPrice").value(BigDecimal.valueOf(250.00)))
                .andExpect(jsonPath("$.currentPrice").value(BigDecimal.valueOf(220.00)))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/updated-image.jpg"));

        verify(productService, times(1)).updateProduct(eq(validProductId), eq(updateRequest));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void updateProduct_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .productName("Updated Product Name")
                .description("Updated Description")
                .listPrice(BigDecimal.valueOf(250.00))
                .currentPrice(BigDecimal.valueOf(220.00))
                .imageUrl("https://example.com/updated-image.jpg")
                .build();

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).updateProduct(any(), any());
    }

    @Test
    void updateProduct_shouldReturnUnauthorized_whenNoAuthentication() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .productName("Updated Product Name")
                .description("Updated Description")
                .listPrice(BigDecimal.valueOf(250.00))
                .currentPrice(BigDecimal.valueOf(220.00))
                .imageUrl("https://example.com/updated-image.jpg")
                .build();

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).updateProduct(any(), any());
    }


    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnBadRequest_whenInvalidProductIdFormat() throws Exception {

        String invalidProductId = "INVALID_UUID";

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .productName("Updated Product Name")
                .description("Updated Description")
                .listPrice(BigDecimal.valueOf(250.00))
                .currentPrice(BigDecimal.valueOf(220.00))
                .imageUrl("https://example.com/updated-image.jpg")
                .build();

        mockMvc.perform(patch("/products/{productId}", invalidProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).updateProduct(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnBadRequest_whenProductNameIsTooShort() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest invalidRequest = ProductUpdateRequest.builder()
                .productName("A")
                .description("Updated Description")
                .listPrice(BigDecimal.valueOf(250.00))
                .currentPrice(BigDecimal.valueOf(220.00))
                .imageUrl("https://example.com/updated-image.jpg")
                .build();

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid name: Must be of 2 - 50 characters")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).updateProduct(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnBadRequest_whenProductNameIsTooLong() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest invalidRequest = ProductUpdateRequest.builder()
                .productName("A".repeat(51))
                .description("Updated Description")
                .listPrice(BigDecimal.valueOf(250.00))
                .currentPrice(BigDecimal.valueOf(220.00))
                .imageUrl("https://example.com/updated-image.jpg")
                .build();

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid name: Must be of 2 - 50 characters")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).updateProduct(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnBadRequest_whenDescriptionIsTooShort() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest invalidRequest = ProductUpdateRequest.builder()
                .productName("Updated Product Name")
                .description("A")
                .listPrice(BigDecimal.valueOf(250.00))
                .currentPrice(BigDecimal.valueOf(220.00))
                .imageUrl("https://example.com/updated-image.jpg")
                .build();

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid description: Must be of 2 - 255 characters")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).updateProduct(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnBadRequest_whenDescriptionIsTooLong() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest invalidRequest = ProductUpdateRequest.builder()
                .productName("Updated Product Name")
                .description("A".repeat(256))
                .listPrice(BigDecimal.valueOf(250.00))
                .currentPrice(BigDecimal.valueOf(220.00))
                .imageUrl("https://example.com/updated-image.jpg")
                .build();

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid description: Must be of 2 - 255 characters")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).updateProduct(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnBadRequest_whenListPriceIsNegative() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest invalidRequest = ProductUpdateRequest.builder()
                .productName("Updated Product Name")
                .description("Updated Description")
                .listPrice(BigDecimal.valueOf(-0.01))
                .currentPrice(BigDecimal.valueOf(220.00))
                .imageUrl("https://example.com/updated-image.jpg")
                .build();

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("List price must be non-negative")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).updateProduct(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnBadRequest_whenListPriceExceedsLimit() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest invalidRequest = ProductUpdateRequest.builder()
                .productName("Updated Product Name")
                .description("Updated Description")
                .listPrice(BigDecimal.valueOf(1000000.00))
                .currentPrice(BigDecimal.valueOf(220.00))
                .imageUrl("https://example.com/updated-image.jpg")
                .build();

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("List price must have up to 6 digits and 2 decimal places","List price must be less than or equal to 999999.99")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).updateProduct(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnBadRequest_whenListPriceHasTooManyFractionDigits() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest invalidRequest = ProductUpdateRequest.builder()
                .productName("Updated Product Name")
                .description("Updated Description")
                .listPrice(BigDecimal.valueOf(123456.789))
                .currentPrice(BigDecimal.valueOf(220.00))
                .imageUrl("https://example.com/updated-image.jpg")
                .build();

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("List price must have up to 6 digits and 2 decimal places")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).updateProduct(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnBadRequest_whenCurrentPriceIsNegative() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest invalidRequest = ProductUpdateRequest.builder()
                .productName("Updated Product Name")
                .description("Updated Description")
                .listPrice(BigDecimal.valueOf(250.00))
                .currentPrice(BigDecimal.valueOf(-0.01))
                .imageUrl("https://example.com/updated-image.jpg")
                .build();

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Current price must be non-negative")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).updateProduct(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnBadRequest_whenCurrentPriceExceedsLimit() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest invalidRequest = ProductUpdateRequest.builder()
                .productName("Updated Product Name")
                .description("Updated Description")
                .listPrice(BigDecimal.valueOf(250.00))
                .currentPrice(BigDecimal.valueOf(1000000.00))
                .imageUrl("https://example.com/updated-image.jpg")
                .build();

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Current price must have up to 6 digits and 2 decimal places","Current price must be less than or equal to 999999.99")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).updateProduct(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnBadRequest_whenCurrentPriceHasTooManyFractionDigits() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest invalidRequest = ProductUpdateRequest.builder()
                .productName("Updated Product Name")
                .description("Updated Description")
                .listPrice(BigDecimal.valueOf(250.00))
                .currentPrice(BigDecimal.valueOf(123456.789))
                .imageUrl("https://example.com/updated-image.jpg")
                .build();

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Current price must have up to 6 digits and 2 decimal places")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).updateProduct(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnBadRequest_whenInvalidImageUrl() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest invalidRequest = ProductUpdateRequest.builder()
                .productName("Updated Product Name")
                .description("Updated Description")
                .listPrice(BigDecimal.valueOf(250.00))
                .currentPrice(BigDecimal.valueOf(220.00))
                .imageUrl("INVALID_URL")
                .build();

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid URL")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).updateProduct(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnBadRequest_whenOnlyProductNameIsProvided() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .productName("Updated Product Name")
                .build();

        ProductResponse expectedResponse = ProductResponse.builder()
                .productId(UUID.fromString(validProductId))
                .productName(updateRequest.getProductName())
                .description("Original Description")
                .listPrice(BigDecimal.valueOf(40.0))
                .currentPrice(BigDecimal.valueOf(30.0))
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl("Original Url")
                .addedAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(productService.updateProduct(eq(validProductId), eq(updateRequest))).thenReturn(expectedResponse);

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(validProductId))
                .andExpect(jsonPath("$.productName").value("Updated Product Name"))
                .andExpect(jsonPath("$.description").value("Original Description"))
                .andExpect(jsonPath("$.listPrice").value(BigDecimal.valueOf(40.0)))
                .andExpect(jsonPath("$.currentPrice").value(BigDecimal.valueOf(30.0)))
                .andExpect(jsonPath("$.imageUrl").value("Original Url"));

        verify(productService, times(1)).updateProduct(eq(validProductId), eq(updateRequest));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnBadRequest_whenOnlyDescriptionIsProvided() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .description("Updated Description")
                .build();

        ProductResponse expectedResponse = ProductResponse.builder()
                .productId(UUID.fromString(validProductId))
                .productName("Original Product Name")
                .description(updateRequest.getDescription())
                .listPrice(BigDecimal.valueOf(40.0))
                .currentPrice(BigDecimal.valueOf(30.0))
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl("Original Url")
                .addedAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(productService.updateProduct(eq(validProductId), eq(updateRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(validProductId))
                .andExpect(jsonPath("$.productName").value("Original Product Name"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.listPrice").value(BigDecimal.valueOf(40.0)))
                .andExpect(jsonPath("$.currentPrice").value(BigDecimal.valueOf(30.0)))
                .andExpect(jsonPath("$.imageUrl").value("Original Url"));

        verify(productService, times(1)).updateProduct(eq(validProductId), eq(updateRequest));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnBadRequest_whenOnlyListPriceIsProvided() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .listPrice(BigDecimal.valueOf(50.0))
                .build();

        ProductResponse expectedResponse = ProductResponse.builder()
                .productId(UUID.fromString(validProductId))
                .productName("Original Product Name")
                .description("Original Description")
                .listPrice(updateRequest.getListPrice())
                .currentPrice(BigDecimal.valueOf(30.0))
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl("Original Url")
                .addedAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(productService.updateProduct(eq(validProductId), eq(updateRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(validProductId))
                .andExpect(jsonPath("$.productName").value("Original Product Name"))
                .andExpect(jsonPath("$.description").value("Original Description"))
                .andExpect(jsonPath("$.listPrice").value(BigDecimal.valueOf(50.0)))
                .andExpect(jsonPath("$.currentPrice").value(BigDecimal.valueOf(30.0)))
                .andExpect(jsonPath("$.imageUrl").value("Original Url"));

        verify(productService, times(1)).updateProduct(eq(validProductId), eq(updateRequest));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnBadRequest_whenOnlyCurrentPriceIsProvided() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .currentPrice(BigDecimal.valueOf(20.0))
                .build();

        ProductResponse expectedResponse = ProductResponse.builder()
                .productId(UUID.fromString(validProductId))
                .productName("Original Product Name")
                .description("Original Description")
                .listPrice(BigDecimal.valueOf(40.0))
                .currentPrice(updateRequest.getCurrentPrice())
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl("Original Url")
                .addedAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(productService.updateProduct(eq(validProductId), eq(updateRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(validProductId))
                .andExpect(jsonPath("$.productName").value("Original Product Name"))
                .andExpect(jsonPath("$.description").value("Original Description"))
                .andExpect(jsonPath("$.listPrice").value(BigDecimal.valueOf(40.0)))
                .andExpect(jsonPath("$.currentPrice").value(BigDecimal.valueOf(20.0)))
                .andExpect(jsonPath("$.imageUrl").value("Original Url"));

        verify(productService, times(1)).updateProduct(eq(validProductId), eq(updateRequest));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void updateProduct_shouldReturnBadRequest_whenOnlyImageUrlIsProvided() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .imageUrl("https://example.com/updated-image.jpg")
                .build();

        ProductResponse expectedResponse = ProductResponse.builder()
                .productId(UUID.fromString(validProductId))
                .productName("Original Product Name")
                .description("Original Description")
                .listPrice(BigDecimal.valueOf(40.0))
                .currentPrice(BigDecimal.valueOf(30.0))
                .productStatus(ProductStatus.AVAILABLE)
                .imageUrl(updateRequest.getImageUrl())
                .addedAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        when(productService.updateProduct(eq(validProductId), eq(updateRequest)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/products/{productId}", validProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(validProductId))
                .andExpect(jsonPath("$.productName").value("Original Product Name"))
                .andExpect(jsonPath("$.description").value("Original Description"))
                .andExpect(jsonPath("$.listPrice").value(BigDecimal.valueOf(40.0)))
                .andExpect(jsonPath("$.currentPrice").value(BigDecimal.valueOf(30.0)))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/updated-image.jpg"));

        verify(productService, times(1)).updateProduct(eq(validProductId), eq(updateRequest));
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void setProductStatus_shouldReturnOk_whenValidRequestAndAdminRole() throws Exception {

        String validProductId = UUID.randomUUID().toString();
        String newStatus = ProductStatus.OUT_OF_STOCK.name();

        MessageResponse expectedMessage = new MessageResponse(String.format("Status '%s' was set for the product with id: %s.", newStatus, validProductId));

        when(productService.setProductStatus(eq(validProductId), eq(newStatus)))
                .thenReturn(expectedMessage);

        mockMvc.perform(patch("/products/{productId}/status", validProductId)
                        .param("productStatus", newStatus)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(expectedMessage.getMessage()));

        verify(productService, times(1)).setProductStatus(eq(validProductId), eq(newStatus));
    }

    @Test
    @WithMockUser(roles = {"CLIENT"})
    void setProductStatus_shouldReturnForbidden_whenUserHasInsufficientRole() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        mockMvc.perform(patch("/products/{productId}/status", validProductId)
                        .param("productStatus", "OUT_OF_STOCK")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AuthorizationDeniedException"))
                .andExpect(jsonPath("$.details").value("Access Denied"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).setProductStatus(any(), any());
    }

    @Test
    void setProductStatus_shouldReturnUnauthorized_whenNoAuthentication() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        mockMvc.perform(patch("/products/{productId}/status", validProductId)
                        .param("productStatus", "SOLD_OUT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("InsufficientAuthenticationException"))
                .andExpect(jsonPath("$.details").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).setProductStatus(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void setProductStatus_shouldReturnOk_whenDefaultStatus() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        String defaultStatus = "AVAILABLE";
        MessageResponse expectedMessage = new MessageResponse(String.format("Status '%s' was set for the product with id: %s.", defaultStatus, validProductId));

        when(productService.setProductStatus(eq(validProductId), eq(defaultStatus)))
                .thenReturn(expectedMessage);

        mockMvc.perform(patch("/products/{productId}/status", validProductId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(expectedMessage.getMessage()));

        verify(productService, times(1)).setProductStatus(eq(validProductId), eq(defaultStatus));
    }


    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void setProductStatus_shouldReturnBadRequest_whenInvalidProductIdFormat() throws Exception {

        mockMvc.perform(patch("/products/{productId}/status", "INVALID_UUID")
                        .param("productStatus", "AVAILABLE")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid UUID format")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).setProductStatus(any(), any());
    }

    @Test
    @WithMockUser(roles = {"ADMINISTRATOR"})
    void setProductStatus_shouldReturnBadRequest_whenInvalidProductStatus() throws Exception {

        String validProductId = UUID.randomUUID().toString();

        mockMvc.perform(patch("/products/{productId}/status", validProductId)
                        .param("productStatus", "INVALID_STATUS")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ConstraintViolationException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid order orderStatus: Must be one of the: 'AVAILABLE', 'OUT_OF_STOCK' or 'SOLD_OUT' ('available', 'out_of_stock' or 'sold_out')")))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(productService, never()).setProductStatus(any(), any());
    }
}